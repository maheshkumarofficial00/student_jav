package com.srmap.booking.config;

import com.srmap.booking.entity.Appointment;
import com.srmap.booking.entity.Booking;
import com.srmap.booking.entity.Resource;
import com.srmap.booking.entity.Room;
import com.srmap.booking.entity.ScheduleBlock;
import com.srmap.booking.entity.User;
import com.srmap.booking.entity.WorkingHour;
import com.srmap.booking.repository.AppointmentRepository;
import com.srmap.booking.repository.BookingRepository;
import com.srmap.booking.repository.ResourceRepository;
import com.srmap.booking.repository.RoomRepository;
import com.srmap.booking.repository.ScheduleBlockRepository;
import com.srmap.booking.repository.UserRepository;
import com.srmap.booking.repository.WorkingHourRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    public static record WorkingTimeSlot(String start, String end) {}

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ResourceRepository resourceRepository;
    private final WorkingHourRepository workingHourRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            RoomRepository roomRepository,
            ResourceRepository resourceRepository,
            WorkingHourRepository workingHourRepository,
            ScheduleBlockRepository scheduleBlockRepository,
            AppointmentRepository appointmentRepository,
            BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.resourceRepository = resourceRepository;
        this.workingHourRepository = workingHourRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.appointmentRepository = appointmentRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Data already initialized
        }

        String encodedPassword = passwordEncoder.encode("Campus@123");

        // 1. Admin
        User admin = User.builder()
                .name("Meera Rao")
                .email("admin@srmap.edu.in")
                .role(User.Role.ADMIN)
                .active(true)
                .password(encodedPassword)
                .build();
        userRepository.save(admin);

        // Helper for weekday ranges
        List<WorkingTimeSlot> fullHours = List.of(new WorkingTimeSlot("09:00", "13:00"), new WorkingTimeSlot("14:00", "17:00"));

        // 2. Eight Core Faculty
        List<User> coreFaculty = new ArrayList<>();

        User f1 = createFaculty("Dr. Jayasree Subramanian", "faculty1@srmap.edu.in", "Discrete Mathematics", "B-210", encodedPassword);
        coreFaculty.add(f1);

        User f2 = createFaculty("Dr.Sugyan Kumar Mishra", "faculty2@srmap.edu.in", "Design and Analysis of Algorithms", "C-105", encodedPassword);
        coreFaculty.add(f2);

        User f3 = createFaculty("Dr. Pudi Dhilleswararao", "faculty3@srmap.edu.in", "Digital Electronics", "A-312", encodedPassword);
        coreFaculty.add(f3);

        User f4 = createFaculty("Dr. Aditya Kumar Sahu", "faculty4@srmap.edu.in", "Advance Java Programming", "D-020", encodedPassword);
        coreFaculty.add(f4);

        User f5 = createFaculty("Dr. Bibaswan Bose", "faculty5@srmap.edu.in", "Foundations of Electrical and Electronics Engineering", "", encodedPassword);
        coreFaculty.add(f5);

        User f6 = createFaculty("Dr. Venkata Ramana Rao", "faculty6@srmap.edu.in", "Foundations of Electrical and Electronics Engineering", "B-215", encodedPassword);
        coreFaculty.add(f6);

        User f7 = createFaculty("Dr. Sneha Priyadarshini", "faculty7@srmap.edu.in", "Digital Electronics", "C-110", encodedPassword);
        coreFaculty.add(f7);

        User f8 = createFaculty("Dr. Kiran Kumar Reddy", "faculty8@srmap.edu.in", "Discrete Mathematics", "A-315", encodedPassword);
        coreFaculty.add(f8);

        userRepository.saveAll(coreFaculty);

        // Save core faculty working hours
        saveWorkingHours(f1, fullHours);
        saveWorkingHours(f2, List.of(new WorkingTimeSlot("10:00", "16:00")));
        saveWorkingHours(f3, List.of(new WorkingTimeSlot("09:30", "12:30"), new WorkingTimeSlot("13:30", "17:30")));
        saveWorkingHours(f4, List.of(new WorkingTimeSlot("11:00", "18:00")));
        saveWorkingHours(f5, fullHours);
        saveWorkingHours(f6, List.of(new WorkingTimeSlot("09:00", "15:00")));
        saveWorkingHours(f7, List.of(new WorkingTimeSlot("10:00", "17:00")));
        saveWorkingHours(f8, List.of(new WorkingTimeSlot("09:30", "16:30")));

        // 3. 15 Extra Faculty
        String[][] newFacultyData = {
                {"Dr. Arun Kumar",     "arun.kumar@srmap.edu.in",     "Discrete Mathematics",                                  "B-301"},
                {"Dr. Priya Sharma",   "priya.sharma@srmap.edu.in",   "Design and Analysis of Algorithms",                     "B-302"},
                {"Dr. Ravi Teja",      "ravi.teja@srmap.edu.in",      "Digital Electronics",                                   "C-201"},
                {"Dr. Sneha Reddy",    "sneha.reddy@srmap.edu.in",    "Advanced Java Programming",                             "D-110"},
                {"Dr. Kiran Kumar",    "kiran.kumar@srmap.edu.in",    "Foundations of Electrical and Electronics Engineering", "C-210"},
                {"Dr. Divya Lakshmi",  "divya.lakshmi@srmap.edu.in",  "Aptitude for Engineers",                                "A-120"},
                {"Dr. Sandeep Rao",    "sandeep.rao@srmap.edu.in",    "Discrete Mathematics",                                  "B-303"},
                {"Dr. Ananya Das",     "ananya.das@srmap.edu.in",     "Design and Analysis of Algorithms",                     "B-304"},
                {"Dr. Naveen Kumar",   "naveen.kumar@srmap.edu.in",   "Digital Electronics",                                   "C-202"},
                {"Dr. Pooja Singh",    "pooja.singh@srmap.edu.in",    "Advanced Java Programming",                             "D-111"},
                {"Dr. Rahul Varma",    "rahul.varma@srmap.edu.in",    "Foundations of Electrical and Electronics Engineering", "C-211"},
                {"Dr. Keerthi Reddy",  "keerthi.reddy@srmap.edu.in",  "Aptitude for Engineers",                                "A-121"},
                {"Dr. Abhishek Rao",   "abhishek.rao@srmap.edu.in",   "Discrete Mathematics",                                  "B-305"},
                {"Dr. Swathi Priya",   "swathi.priya@srmap.edu.in",   "Design and Analysis of Algorithms",                     "B-306"},
                {"Dr. Manoj Kumar",    "manoj.kumar@srmap.edu.in",    "Digital Electronics",                                   "C-203"}
        };

        List<List<WorkingTimeSlot>> newFacultyHours = List.of(
                List.of(new WorkingTimeSlot("09:00", "13:00"), new WorkingTimeSlot("14:00", "17:00")),
                List.of(new WorkingTimeSlot("10:00", "16:00")),
                List.of(new WorkingTimeSlot("09:30", "12:30"), new WorkingTimeSlot("13:30", "17:30")),
                List.of(new WorkingTimeSlot("11:00", "18:00")),
                List.of(new WorkingTimeSlot("09:00", "15:00"))
        );

        List<User> extraFaculty = new ArrayList<>();
        for (int i = 0; i < newFacultyData.length; i++) {
            String[] d = newFacultyData[i];
            User fac = createFaculty(d[0], d[1], d[2], d[3], encodedPassword);
            extraFaculty.add(fac);
        }
        userRepository.saveAll(extraFaculty);

        for (int i = 0; i < extraFaculty.size(); i++) {
            saveWorkingHours(extraFaculty.get(i), newFacultyHours.get(i % newFacultyHours.size()));
        }

        // 4. Students
        User s1 = User.builder()
                .name("Priya Sharma")
                .email("student1@srmap.edu.in")
                .role(User.Role.STUDENT)
                .active(true)
                .password(encodedPassword)
                .build();
        User s2 = User.builder()
                .name("Karthik Reddy")
                .email("student2@srmap.edu.in")
                .role(User.Role.STUDENT)
                .active(true)
                .password(encodedPassword)
                .build();
        userRepository.saveAll(List.of(s1, s2));

        // 5. Rooms
        List<Room> rooms = new ArrayList<>();
        Room r1 = Room.builder().name("A-101").building("Academic Block A").type(Room.RoomType.CLASSROOM).capacity(60).features("projector,whiteboard").active(true).build();
        Room r2 = Room.builder().name("A-204").building("Academic Block A").type(Room.RoomType.CLASSROOM).capacity(30).features("projector").active(true).build();
        Room r3 = Room.builder().name("Meeting Room M1").building("Library").type(Room.RoomType.MEETING_ROOM).capacity(8).features("whiteboard,video conferencing").active(true).build();
        Room r4 = Room.builder().name("Meeting Room M2").building("Library").type(Room.RoomType.MEETING_ROOM).capacity(12).features("projector").active(true).build();
        Room r5 = Room.builder().name("Seminar Hall S1").building("Main Building").type(Room.RoomType.SEMINAR_HALL).capacity(150).features("projector,sound system").active(true).build();

        rooms.add(r1);
        rooms.add(r2);
        rooms.add(r3);
        rooms.add(r4);
        rooms.add(r5);

        // Generate 297 campus rooms
        // SR Block: floors 2..7, 15 rooms/floor, cap 60
        generateBlockRooms(rooms, "SR Block", "S", 2, 7, 15, 60);
        // V Block: floors 1..6, 12 rooms/floor, cap 40
        generateBlockRooms(rooms, "V Block", "V", 1, 6, 12, 40);
        // CV Raman Block: floors 2..10, 15 rooms/floor, cap 50
        generateBlockRooms(rooms, "CV Raman Block", "C", 2, 10, 15, 50);

        roomRepository.saveAll(rooms);

        // 6. Resources
        Resource res1 = Resource.builder().name("Portable projector").category("AV equipment").description("HDMI and VGA, carry case included.").active(true).build();
        Resource res2 = Resource.builder().name("Laptop cart (20 units)").category("Computing").description("Charging cart with 20 laptops.").active(true).build();
        Resource res3 = Resource.builder().name("Video conferencing kit").category("AV equipment").description("Camera, speakerphone and stand.").active(true).build();
        Resource res4 = Resource.builder().name("3D printer").category("Lab equipment").description("Fabrication lab, trained users only.").active(true).build();
        resourceRepository.saveAll(List.of(res1, res2, res3, res4));

        // 7. Initial Block for f1
        LocalDate tomorrow = LocalDate.now().plusDays(2);
        ScheduleBlock block = ScheduleBlock.builder()
                .faculty(f1)
                .date(tomorrow)
                .startTime("09:00")
                .endTime("12:00")
                .reason("Conference")
                .build();
        scheduleBlockRepository.save(block);

        // 8. Initial Appointments
        LocalDate day1 = LocalDate.now().plusDays(1);
        LocalDate day2 = LocalDate.now().plusDays(2);

        Appointment a1 = Appointment.builder()
                .student(s1)
                .faculty(f1)
                .room(r3)
                .date(day1)
                .startTime("10:00")
                .endTime("11:00")
                .purpose("Project review")
                .status(Appointment.Status.CONFIRMED)
                .build();

        Appointment a2 = Appointment.builder()
                .student(s1)
                .faculty(f1)
                .room(null)
                .date(day1)
                .startTime("14:00")
                .endTime("14:30")
                .purpose("Reference letter")
                .status(Appointment.Status.CONFIRMED)
                .build();

        Appointment a3 = Appointment.builder()
                .student(s2)
                .faculty(f2)
                .room(r4)
                .date(day2)
                .startTime("11:00")
                .endTime("12:00")
                .purpose("Thesis outline")
                .status(Appointment.Status.PENDING)
                .build();

        appointmentRepository.saveAll(List.of(a1, a2, a3));

        // 9. Initial Bookings
        Booking b1 = Booking.builder()
                .user(f2)
                .room(r3)
                .resources(List.of())
                .date(day1)
                .startTime("11:00")
                .endTime("12:30")
                .purpose("Lab group meeting")
                .status(Booking.Status.CONFIRMED)
                .build();

        Booking b2 = Booking.builder()
                .user(f3)
                .room(r4)
                .resources(List.of(res1))
                .date(day1)
                .startTime("13:00")
                .endTime("16:00")
                .purpose("Workshop prep")
                .status(Booking.Status.CONFIRMED)
                .build();

        bookingRepository.saveAll(List.of(b1, b2));
    }

    private User createFaculty(String name, String email, String department, String office, String encodedPassword) {
        return User.builder()
                .name(name)
                .email(email)
                .role(User.Role.FACULTY)
                .active(true)
                .password(encodedPassword)
                .department(department)
                .office(office)
                .title("Faculty")
                .subjects(department)
                .build();
    }

    private void saveWorkingHours(User faculty, List<WorkingTimeSlot> slots) {
        List<WorkingHour> list = new ArrayList<>();
        for (int d = 1; d <= 5; d++) {
            for (WorkingTimeSlot slot : slots) {
                list.add(WorkingHour.builder()
                        .faculty(faculty)
                        .dayOfWeek(d)
                        .startTime(slot.start())
                        .endTime(slot.end())
                        .build());
            }
        }
        workingHourRepository.saveAll(list);
    }

    private void generateBlockRooms(List<Room> list, String blockName, String prefix, int startFloor, int endFloor, int roomsPerFloor, int capacity) {
        for (int floor = startFloor; floor <= endFloor; floor++) {
            for (int n = 1; n <= roomsPerFloor; n++) {
                String roomNumber = String.format("%s%d%02d", prefix, floor, n);
                list.add(Room.builder()
                        .name(roomNumber)
                        .block(blockName)
                        .floor(floor)
                        .building(blockName + ", Floor " + floor)
                        .type(Room.RoomType.CLASSROOM)
                        .capacity(capacity)
                        .features("projector,whiteboard")
                        .active(true)
                        .build());
            }
        }
    }
}
