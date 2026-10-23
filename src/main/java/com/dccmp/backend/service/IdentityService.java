package com.dccmp.backend.service;
import com.dccmp.backend.entity.*;
import com.dccmp.backend.repository.*;
import com.dccmp.backend.dto.Records;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service public class IdentityService {
 private final StudentRepository students; private final OtpRepository otps; private final StaffRepository staff; private final JavaMailSender mail;private final PasswordEncoder encoder;
 private final SecureRandom random=new SecureRandom();
 @Value("${app.student-roll-regex}") String rollRegex; @Value("${app.mail-from}") String mailFrom;
 public IdentityService(StudentRepository a,OtpRepository b,StaffRepository c,JavaMailSender d,PasswordEncoder e){students=a;otps=b;staff=c;mail=d;encoder=e;}
 public static String normalizedRoll(String roll){return Objects.toString(roll,"").trim().toUpperCase(Locale.ROOT);}
 public void send(Records.OtpStart start){
  String roll=normalizedRoll(start.rollNumber());String email=Objects.toString(start.email(),"").trim().toLowerCase(Locale.ROOT);
  if(!roll.matches(rollRegex)||!email.equals(roll.toLowerCase(Locale.ROOT)+"@usharama.in"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Roll number and official email do not match");
  LocalDateTime now=LocalDateTime.now();List<Otp> old=otps.findTop10ByEmailOrderByCreatedAtDesc(email);
  if(!old.isEmpty()&&old.getFirst().createdAt!=null&&old.getFirst().createdAt.isAfter(now.minusSeconds(60)))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Wait one minute before requesting another OTP");
  String token=String.format("%06d",random.nextInt(1000000));
  Otp o=new Otp();o.email=email;o.hash=encoder.encode(token);o.attempts=0;o.createdAt=now;o.expiresAt=now.plusMinutes(10);otps.save(o);
  SimpleMailMessage msg=new SimpleMailMessage();msg.setFrom(mailFrom);msg.setTo(email);msg.setSubject("DCCMP verification code");msg.setText("Your DCCMP verification code: "+token+"\nExpires in 10 minutes. Do not share this code.");
  try{mail.send(msg);}catch(Exception ex){o.consumedAt=LocalDateTime.now();otps.save(o);throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Mail sending failed; check SMTP setup");}
 }
 public Student verify(Records.OtpCheck input,HttpServletRequest req){
  String roll=normalizedRoll(input.rollNumber());if(!roll.matches(rollRegex))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid roll number");
  String email=roll.toLowerCase(Locale.ROOT)+"@usharama.in";
  Otp o=otps.findTop10ByEmailOrderByCreatedAtDesc(email).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Request an OTP first"));
  if(o.consumedAt!=null||o.expiresAt.isBefore(LocalDateTime.now())||o.attempts>=5)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"OTP expired, used, or locked");
  o.attempts++; if(!encoder.matches(Objects.toString(input.otp(),""),o.hash)){otps.save(o);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid OTP");}
  o.consumedAt=LocalDateTime.now();otps.save(o);
  Student s=students.findByRollNumber(roll).orElseGet(Student::new);s.rollNumber=roll;s.email=email;s.verifiedAt=LocalDateTime.now();s=students.save(s);
  req.getSession(true).setAttribute("studentId",s.id);req.getSession().removeAttribute("staffId");req.getSession().removeAttribute("role");req.changeSessionId();return s;
 }
 public Map<String,Object> login(Records.Login login,HttpServletRequest req){
  String code=Objects.toString(login.code(),"").trim().toUpperCase(Locale.ROOT);
  Staff a=staff.findByCode(code).orElse(null);
  if(a==null||!Boolean.TRUE.equals(a.active)||!encoder.matches(Objects.toString(login.password(),""),a.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid staff credentials");
  req.getSession(true).setAttribute("staffId",a.id);req.getSession().setAttribute("role",a.role);req.getSession().removeAttribute("studentId");req.changeSessionId();
  return Map.of("id",a.id,"name",a.name,"code",a.code,"role",a.role,"departmentId",a.departmentId==null?0:a.departmentId);
 }
}
