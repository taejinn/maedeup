package com.example.maedeup;

import com.example.maedeup.entity.RoleType;
import com.example.maedeup.entity.User;
import com.example.maedeup.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableJpaAuditing
@SpringBootApplication
public class MaedeupApplication {

    public static void main(String[] args) {
        loadEnvironmentVariables();
        SpringApplication.run(MaedeupApplication.class, args);
    }

    private static void loadEnvironmentVariables() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
            
            dotenv.entries().forEach(entry -> 
                System.setProperty(entry.getKey(), entry.getValue())
            );
        } catch (Exception e) {
            System.out.println(".env 파일을 찾을 수 없습니다. 환경변수나 기본값을 사용합니다.");
        }
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 테스트용 일반 사용자 생성
            if (userRepository.findByLoginId("maedeup").isEmpty()) {
                User testUser = new User(
                        "maedeup",
                        "테스트사용자",
                        "test@example.com",
                        passwordEncoder.encode("1234"),
                        RoleType.USER
                );
                userRepository.save(testUser);
                System.out.println("테스트 사용자 생성: maedeup/1234 (USER)");
            }

            // 테스트용 관리자 사용자 생성
            if (userRepository.findByLoginId("admin").isEmpty()) {
                User adminUser = new User(
                        "admin",
                        "관리자",
                        "admin@example.com",
                        passwordEncoder.encode("admin"),
                        RoleType.ADMIN
                );
                userRepository.save(adminUser);
                System.out.println("테스트 관리자 생성: admin/admin (ADMIN)");
            }
        };
    }
}
