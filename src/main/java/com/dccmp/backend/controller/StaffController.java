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
public class StaffController {
 private final Access access;
 private final StaffRepository staff;
 private final ApprovalRepository approvals;
 private final WorkflowService flow;
 private final DepartmentRepository departments;
 private final AlumniRepository alumni;
 private final FriendRepository friends;
 public StaffController(Access access, StaffRepository staff, ApprovalRepository approvals, WorkflowService flow, DepartmentRepository departments, AlumniRepository alumni, FriendRepository friends){
  this.access=access;
  this.staff=staff;
  this.approvals=approvals;
  this.flow=flow;
  this.departments=departments;
  this.alumni=alumni;
  this.friends=friends;
 }

 @GetMapping("/staff/queue") public List<Records.ApplicationView> staffQueue(HttpServletRequest req){Staff s=access.currentStaff(req,staff,"STAFF");if(s.departmentId==null)return List.of();return approvals.findByDepartmentIdOrderByIdDesc(s.departmentId).stream().map(x->flow.getApp(x.applicationId)).map(flow::view).toList();}
 @PatchMapping("/staff/approvals/{id}/approve") public Records.ApplicationView staffApprove(@PathVariable long id,@RequestBody(required=false) Records.DecisionInput input,HttpServletRequest req){Staff actor=access.currentStaff(req,staff,"STAFF");return flow.view(flow.decideDepartment(actor,id,true,input==null?"":input.remarks()));}
 @PatchMapping("/staff/approvals/{id}/reject") public Records.ApplicationView staffReject(@PathVariable long id,@RequestBody Records.DecisionInput input,HttpServletRequest req){Staff actor=access.currentStaff(req,staff,"STAFF");return flow.view(flow.decideDepartment(actor,id,false,input.remarks()));}
 @GetMapping("/staff/alumni") public List<Map<String,Object>> alumniRecords(HttpServletRequest req){Staff actor=access.currentStaff(req,staff,"STAFF");Department d=departments.findById(actor.departmentId==null?-1:actor.departmentId).orElseThrow(()->WorkflowService.bad("Not assigned"));if(!"alumni".equals(d.code))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Only Alumni department staff");return alumni.findAll().stream().map(a->Map.<String,Object>of("registration",a,"friends",friends.findByAlumniIdOrderBySlot(a.id))).toList();}
}
