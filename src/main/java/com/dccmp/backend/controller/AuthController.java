package com.dccmp.backend.controller;

import com.dccmp.backend.dto.Records;
import com.dccmp.backend.entity.*;
import com.dccmp.backend.repository.*;
import com.dccmp.backend.service.*;
import com.dccmp.backend.security.Access;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final IdentityService identity;
    private final Access access;
    private final StaffRepository staff;

    public AuthController(IdentityService identity, Access access, StaffRepository staff) {
        this.identity = identity;
        this.access = access;
        this.staff = staff;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Records.Login in, HttpServletRequest req) {
        return identity.login(in, req);
    }

    @PostMapping("/auth/logout")
    public Map<String, Boolean> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(HttpServletRequest req) {
        Staff s = access.currentStaff(req, staff, "STAFF", "PRINCIPAL", "ADMIN");
        return profile(s);
    }

    private Map<String, Object> profile(Staff s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.id);
        m.put("code", s.code);
        m.put("name", s.name);
        m.put("role", s.role);
        m.put("departmentId", s.departmentId);
        m.put("active", s.active);
        return m;
    }
}
