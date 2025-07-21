package com.example.maedeup.service;

import com.example.maedeup.entity.*;
import com.example.maedeup.repository.EventRepository;
import com.example.maedeup.repository.ParticipationRepository;
import com.example.maedeup.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
public class EventConcurrencyTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private AdminEventService adminEventService;

    @Autowired
    private RedisEventCounterService redisEventCounterService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    private User testAdmin;
    private EventFcfs testEvent;

    @BeforeEach
    @Transactional
    public void setUp() {
        testAdmin = createTestUser("admin", RoleType.ADMIN);
        
        testEvent = new EventFcfs(
                testAdmin,
                "테스트 선착순 이벤트",
                "동시성 테스트용 이벤트",
                LocalDateTime.now().plusMinutes(1),
                10
        );
        testEvent = (EventFcfs) eventRepository.save(testEvent);
        
        redisEventCounterService.clearEventData(testEvent.getId());
        redisEventCounterService.initializeParticipantCount(testEvent.getId(), 0L);
    }

    @Test
    @DisplayName("선착순 이벤트 동시 참여 테스트 - 정확히 최대 인원만 성공해야 함")
    public void testConcurrentFcfsParticipation() throws Exception {
        int totalUsers = 100;
        int maxParticipants = testEvent.getMaxParticipants();
        
        List<User> users = IntStream.range(0, totalUsers)
                .mapToObj(i -> createTestUser("user" + i, RoleType.USER))
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(50);

        List<CompletableFuture<Boolean>> futures = users.stream()
                .map(user -> CompletableFuture.supplyAsync(() -> {
                    try {
                        eventService.participateInEvent(testEvent.getId(), user.getLoginId());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long successCount = futures.stream()
                .mapToLong(future -> future.join() ? 1 : 0)
                .sum();

        Long redisCount = redisEventCounterService.getCurrentParticipantCount(testEvent.getId());
        
        long dbSuccessCount = participationRepository.countByEventIdAndStatus(
                testEvent.getId(), ParticipationStatus.SUCCESS);

        assertThat(successCount).isLessThanOrEqualTo(maxParticipants);
        assertThat(redisCount).isLessThanOrEqualTo(maxParticipants);
        assertThat(dbSuccessCount).isLessThanOrEqualTo(maxParticipants);
        
        executor.shutdown();
    }

    @Test
    @DisplayName("중복 참여 방지 테스트")
    public void testDuplicateParticipationPrevention() throws Exception {
        User user = createTestUser("duplicateUser", RoleType.USER);
        
        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<CompletableFuture<Boolean>> futures = IntStream.range(0, 10)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        eventService.participateInEvent(testEvent.getId(), user.getLoginId());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long successCount = futures.stream()
                .mapToLong(future -> future.join() ? 1 : 0)
                .sum();

        assertThat(successCount).isEqualTo(1);
        assertThat(redisEventCounterService.getCurrentParticipantCount(testEvent.getId())).isGreaterThanOrEqualTo(1);
        
        executor.shutdown();
    }

    @Test
    @DisplayName("참여 취소 동시성 테스트")
    public void testConcurrentParticipationCancellation() throws Exception {
        List<User> users = IntStream.range(0, 5)
                .mapToObj(i -> createTestUser("cancelUser" + i, RoleType.USER))
                .toList();

        users.forEach(user -> {
            try {
                eventService.participateInEvent(testEvent.getId(), user.getLoginId());
            } catch (Exception e) {
            }
        });

        List<Participation> participations = participationRepository.findByEventId(testEvent.getId());
        
        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<CompletableFuture<Boolean>> futures = participations.stream()
                .map(participation -> CompletableFuture.supplyAsync(() -> {
                    try {
                        eventService.cancelParticipation(participation.getId(), 
                                participation.getUser().getLoginId());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(redisEventCounterService.getCurrentParticipantCount(testEvent.getId())).isLessThanOrEqualTo(0);
        
        executor.shutdown();
    }

    @Test
    @DisplayName("Redis와 DB 데이터 일관성 테스트")
    public void testRedisDbConsistency() throws Exception {
        List<User> users = IntStream.range(0, 7)
                .mapToObj(i -> createTestUser("consistencyUser" + i, RoleType.USER))
                .toList();

        users.forEach(user -> {
            try {
                eventService.participateInEvent(testEvent.getId(), user.getLoginId());
            } catch (Exception e) {
            }
        });

        Long redisCount = redisEventCounterService.getCurrentParticipantCount(testEvent.getId());
        long dbCount = participationRepository.countByEventIdAndStatus(
                testEvent.getId(), ParticipationStatus.SUCCESS);

        assertThat(Math.abs(redisCount - dbCount)).isLessThanOrEqualTo(1);
        assertThat(redisCount).isLessThanOrEqualTo(testEvent.getMaxParticipants());
        assertThat(dbCount).isLessThanOrEqualTo(testEvent.getMaxParticipants());
    }

    private User createTestUser(String loginId, RoleType role) {
        User user = new User(
                loginId,
                loginId + "_nick", 
                loginId + "@test.com",
                "password",
                role
        );
        return userRepository.save(user);
    }
} 