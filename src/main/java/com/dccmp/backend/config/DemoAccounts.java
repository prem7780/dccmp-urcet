
package com.dccmp.backend.config;

import com.dccmp.backend.entity.Department;
import com.dccmp.backend.entity.Staff;
import com.dccmp.backend.repository.DepartmentRepository;
import com.dccmp.backend.repository.StaffRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoAccounts {

 @Bean
 public CommandLineRunner seedDemoAccounts(
         StaffRepository staff,
         DepartmentRepository departments,
         PasswordEncoder encoder,
         @Value("${DEMO_SEED_STAFF:false}") boolean enabled,
         @Value("${DEMO_STAFF_PASSWORD:}") String password
 ) {

  return args -> {

   if (!enabled) {
    return;
   }

   if (password.length() < 12) {
    throw new IllegalArgumentException(
            "DEMO_STAFF_PASSWORD must have at least 12 characters"
    );
   }

   // Create demo accounts for all 17 departments
   for (Department department : departments.findAll()) {

    String code = "DEMO_" + department.code.toUpperCase();

    if (staff.findByCode(code).isPresent()) {
     continue;
    }

    Staff newStaff = new Staff();

    newStaff.code = code;
    newStaff.name = department.name + " Officer";
    newStaff.role = "STAFF";
    newStaff.departmentId = department.id;
    newStaff.passwordHash = encoder.encode(password);
    newStaff.active = true;

    staff.save(newStaff);
   }

   // Create demo Principal account
   if (staff.findByCode("DEMO_PRINCIPAL").isEmpty()) {

    Staff principal = new Staff();

    principal.code = "DEMO_PRINCIPAL";
    principal.name = "Principal Demo";
    principal.role = "PRINCIPAL";
    principal.passwordHash = encoder.encode(password);
    principal.active = true;

    staff.save(principal);
   }

   System.out.println(
           "DCCMP: Local demo staff accounts enabled. " +
                   "Do not enable in production."
   );
  };
 }
}
