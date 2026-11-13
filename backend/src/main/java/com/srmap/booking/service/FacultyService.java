package com.srmap.booking.service;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.FacultyDto;
import com.srmap.booking.dto.ScheduleBlockRequest;
import com.srmap.booking.dto.ScheduleResponse;
import com.srmap.booking.dto.WorkingHoursRequest;
import com.srmap.booking.entity.Appointment;
import com.srmap.booking.entity.ScheduleBlock;
import com.srmap.booking.entity.User;
import com.srmap.booking.entity.WorkingHour;
import com.srmap.booking.repository.AppointmentRepository;
import com.srmap.booking.repository.ScheduleBlockRepository;
import com.srmap.booking.repository.UserRepository;
import com.srmap.booking.repository.WorkingHourRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class FacultyService {

    private final UserRepository userRepository;
    private final WorkingHourRepository workingHourRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final TimeIntervalService timeIntervalService;

    public FacultyService(
            UserRepository userRepository,
            WorkingHourRepository workingHourRepository,
            ScheduleBlockRepository scheduleBlockRepository,
            AppointmentRepository appointmentRepository,
            AvailabilityService availabilityService,
            TimeIntervalService timeIntervalService) {
        this.userRepository = userRepository;
        this.workingHourRepository = workingHourRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.timeIntervalService = timeIntervalService;
    }

    @Transactional(readOnly = true)
    public List<FacultyDto> getAllActiveFaculty() {
        return userRepository.findByRoleAndActiveTrue(User.Role.FACULTY).stream()
                .map(FacultyDto::from)
                .toList();
    }

    private User checkScheduleOwner(Long facultyId, User currentUser) {
        if (currentUser.getRole() != User.Role.ADMIN && !currentUser.getId().equals(facultyId)) {
            throw ApiException.forbidden("You can only manage your own schedule.");
        }
        return userRepository.findById(facultyId)
                .filter(u -> u.getRole() == User.Role.FACULTY)
                .orElseThrow(() -> ApiException.notFound("Faculty member not found."));
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(Long facultyId, User currentUser) {
        User faculty = checkScheduleOwner(facultyId, currentUser);

        Map<Integer, List<ScheduleResponse.TimeRangeDto>> whMap = new HashMap<>();
        for (int d = 0; d < 7; d++) {
            whMap.put(d, new ArrayList<>());
        }

        List<WorkingHour> whs = workingHourRepository.findByFacultyId(faculty.getId());
        for (WorkingHour wh : whs) {
            whMap.computeIfAbsent(wh.getDayOfWeek(), k -> new ArrayList<>())
                    .add(new ScheduleResponse.TimeRangeDto(wh.getStartTime(), wh.getEndTime()));
        }

        for (int d = 0; d < 7; d++) {
            whMap.get(d).sort(Comparator.comparing(ScheduleResponse.TimeRangeDto::getStart));
        }

        List<ScheduleResponse.ScheduleBlockDto> blocks = scheduleBlockRepository.findByFacultyId(faculty.getId()).stream()
                .sorted(Comparator.comparing(b -> b.getDate().toString() + b.getStartTime()))
                .map(b -> new ScheduleResponse.ScheduleBlockDto(
                        b.getId(),
                        b.getDate().toString(),
                        b.getStartTime(),
                        b.getEndTime(),
                        b.getReason() != null ? b.getReason() : ""
                ))
                .toList();

        return new ScheduleResponse(whMap, blocks);
    }

    @Transactional
    public Map<String, Object> updateWorkingHours(Long facultyId, WorkingHoursRequest req, User currentUser) {
        User faculty = checkScheduleOwner(facultyId, currentUser);

        Map<String, List<WorkingHoursRequest.TimeRangeDto>> inputMap = req.getWorkingHours();
        if (inputMap == null) inputMap = Collections.emptyMap();

        Map<Integer, List<WorkingHour>> validated = new HashMap<>();

        for (int d = 0; d < 7; d++) {
            List<WorkingHoursRequest.TimeRangeDto> ranges = inputMap.get(String.valueOf(d));
            if (ranges == null) ranges = Collections.emptyList();

            List<WorkingHoursRequest.TimeRangeDto> copy = new ArrayList<>(ranges);
            for (WorkingHoursRequest.TimeRangeDto r : copy) {
                availabilityService.checkTimes("2000-01-01", r.getStart(), r.getEnd());
            }

            copy.sort(Comparator.comparingInt(r -> timeIntervalService.toMin(r.getStart())));

            for (int i = 1; i < copy.size(); i++) {
                WorkingHoursRequest.TimeRangeDto prev = copy.get(i - 1);
                WorkingHoursRequest.TimeRangeDto curr = copy.get(i);
                if (timeIntervalService.overlaps(prev.getStart(), prev.getEnd(), curr.getStart(), curr.getEnd())) {
                    throw ApiException.badRequest("Working hours on one day must not overlap.");
                }
            }

            List<WorkingHour> entities = new ArrayList<>();
            for (WorkingHoursRequest.TimeRangeDto r : copy) {
                entities.add(WorkingHour.builder()
                        .faculty(faculty)
                        .dayOfWeek(d)
                        .startTime(r.getStart())
                        .endTime(r.getEnd())
                        .build());
            }
            validated.put(d, entities);
        }

        workingHourRepository.deleteByFacultyId(faculty.getId());

        List<WorkingHour> toSave = new ArrayList<>();
        validated.values().forEach(toSave::addAll);
        workingHourRepository.saveAll(toSave);

        Map<Integer, List<ScheduleResponse.TimeRangeDto>> respMap = new HashMap<>();
        for (int d = 0; d < 7; d++) {
            List<ScheduleResponse.TimeRangeDto> dtos = validated.get(d).stream()
                    .map(wh -> new ScheduleResponse.TimeRangeDto(wh.getStartTime(), wh.getEndTime()))
                    .toList();
            respMap.put(d, dtos);
        }

        return Map.of("workingHours", respMap);
    }

    @Transactional
    public ScheduleResponse.ScheduleBlockDto addBlock(Long facultyId, ScheduleBlockRequest req, User currentUser) {
        User faculty = checkScheduleOwner(facultyId, currentUser);

        String dateStr = req.getDate();
        String start = req.getStart();
        String end = req.getEnd();
        String reason = req.getReason() != null ? req.getReason().trim() : "";

        availabilityService.checkTimes(dateStr, start, end);
        LocalDate date = AvailabilityService.parseAndValidateDate(dateStr);

        List<Appointment> clashes = appointmentRepository.findOverlappingForFaculty(faculty.getId(), date, start, end);
        if (!clashes.isEmpty()) {
            throw ApiException.conflict("That time overlaps an existing appointment. Cancel or move the appointment first.");
        }

        ScheduleBlock block = ScheduleBlock.builder()
                .faculty(faculty)
                .date(date)
                .startTime(start)
                .endTime(end)
                .reason(reason)
                .build();

        block = scheduleBlockRepository.save(block);

        return new ScheduleResponse.ScheduleBlockDto(block.getId(), dateStr, start, end, reason);
    }

    @Transactional
    public Map<String, Object> deleteBlock(Long facultyId, Long blockId, User currentUser) {
        checkScheduleOwner(facultyId, currentUser);

        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .filter(b -> b.getFaculty().getId().equals(facultyId))
                .orElse(null);

        if (block != null) {
            scheduleBlockRepository.delete(block);
        }

        return Collections.emptyMap();
    }
}
