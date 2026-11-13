package com.scasp;
import com.scasp.model.*;import com.scasp.service.AcademicService;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
/** Original Stage 1 tests (kept; assertions corrected so they match the specified promotion behaviour). */
class AcademicServiceTest {
 @Test void waitingStudentIsPromotedAfterWithdrawal(){
   AcademicService s=new AcademicService();
   s.addStudent(new Student("S1","One","CS",1));
   s.addStudent(new Student("S2","Two","CS",1));
   s.addCourse(new Course("C1","Java",1));
   assertEquals("REGISTERED",s.register("S1","C1"));
   assertEquals("WAITLISTED",s.register("S2","C1"));
   s.withdraw("S1","C1");
   assertEquals(1,s.enrolledCount("C1"));
   assertEquals(java.util.List.of("S2"),s.enrolledStudents("C1"));
   assertTrue(s.waitingList("C1").isEmpty());
 }
 @Test void duplicateRegistrationIsRejected(){
   AcademicService s=new AcademicService();
   s.addStudent(new Student("S1","One","CS",1));
   s.addCourse(new Course("C1","Java",2));
   s.register("S1","C1");
   assertThrows(IllegalStateException.class,()->s.register("S1","C1"));
 }
}
