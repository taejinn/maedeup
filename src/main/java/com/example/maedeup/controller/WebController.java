package com.example.maedeup.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.maedeup.dto.EventListResponseDto;
import com.example.maedeup.dto.SignupRequestDto;
import com.example.maedeup.service.AdminEventService;
import com.example.maedeup.service.EventService;
import com.example.maedeup.service.UserService;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final EventService eventService;
    private final AdminEventService adminEventService;
    private final UserService userService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/events")
    public String events(Model model, 
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        model.addAttribute("events", eventService.getAllEvents(pageable));
        return "events/list";
    }

    @GetMapping("/events/{id}")
    public String eventDetail(@PathVariable Long id, Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("event", eventService.getEventDetails(id));
        
        if (userDetails != null) {
            try {
                model.addAttribute("participation", 
                    eventService.getMyParticipationForEvent(id, userDetails.getUsername()));
            } catch (Exception e) {
                model.addAttribute("participation", null);
            }
        }
        return "events/detail";
    }

    @GetMapping("/my")
    public String myPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("participations", 
            eventService.getMyParticipations(userDetails.getUsername()));
        model.addAttribute("notifications",
            eventService.getMyNotifications(userDetails.getUsername()));
        model.addAttribute("username", userDetails.getUsername());
        return "my/page";
    }

    @GetMapping("/admin")
    public String adminDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        if (!userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        return "admin/dashboard";
    }

    @GetMapping("/admin/events")
    public String adminEvents(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventListResponseDto> eventsPage = eventService.getAllEvents(pageable);
        
        // 각 이벤트의 참여자 수 조회
        Map<Long, Long> participantCounts = new HashMap<>();
        for (EventListResponseDto event : eventsPage.getContent()) {
            long count = adminEventService.getEventParticipants(event.eventId()).size();
            participantCounts.put(event.eventId(), count);
        }
        
        model.addAttribute("events", eventsPage);
        model.addAttribute("participantCounts", participantCounts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventsPage.getTotalPages());
        model.addAttribute("hasNext", eventsPage.hasNext());
        model.addAttribute("hasPrevious", eventsPage.hasPrevious());
        return "admin/events";
    }

    @GetMapping("/admin/events/create")
    public String createEventForm() {
        return "admin/create-event";
    }

    @GetMapping("/admin/events/{id}")
    public String adminEventDetail(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getEventDetails(id));
        model.addAttribute("participants", adminEventService.getEventParticipants(id));
        return "admin/event-detail";
    }

    @GetMapping("/admin/events/{id}/edit")
    public String editEventForm(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getEventDetails(id));
        return "admin/edit-event";
    }

    @GetMapping("/admin/send-notification")
    public String sendNotificationForm(Model model) {
        model.addAttribute("events", eventService.getAllEvents(PageRequest.of(0, 100)).getContent());
        return "admin/send-notification";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupRequest", new SignupRequestDto("", "", "", "", "", null));
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupRequestDto signupRequest, 
                        RedirectAttributes redirectAttributes) {
        try {
            userService.signup(signupRequest);
            redirectAttributes.addFlashAttribute("successMessage", 
                "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/signup";
        }
    }
} 