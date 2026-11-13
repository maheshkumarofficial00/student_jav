package com.srmap.booking.service;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.AvailabilityResponse;
import com.srmap.booking.dto.CombinedAvailabilityRequest;
import com.srmap.booking.dto.CombinedAvailabilityResponse;
import com.srmap.booking.entity.*;
import com.srmap.booking.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AvailabilityService {

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}$");
    private static final int MAX_ROOMS_PER_DAY = 6;

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ResourceRepository resourceRepository;
    private final WorkingHourRepository workingHourRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final BookingRepository bookingRepository;
    private final TimeIntervalService timeIntervalService;

    @Value("${app.max-search-days:14}")
    private int maxSearchDays;

    public AvailabilityService(
            UserRepository userRepository,
            RoomRepository roomRepository,
            ResourceRepository resourceRepository,
            WorkingHourRepository workingHourRepository,
            ScheduleBlockRepository scheduleBlockRepository,
            AppointmentRepository appointmentRepository,
            BookingRepository bookingRepository,
            TimeIntervalService timeIntervalService) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.resourceRepository = resourceRepository;
        this.workingHourRepository = workingHourRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.appointmentRepository = appointmentRepository;
        this.bookingRepository = bookingRepository;
        this.timeIntervalService = timeIntervalService;
    }

    public static LocalDate parseAndValidateDate(String dateStr) {
        if (dateStr == null || !DATE_PATTERN.matcher(dateStr.trim()).matches()) {
            throw ApiException.badRequest("A date in YYYY-MM-DD format is required.");
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("A date in YYYY-MM-DD format is required.");
        }
    }

    public void checkTimes(String dateStr, String start, String end) {
        if (dateStr == null || !DATE_PATTERN.matcher(dateStr.trim()).matches() ||
                start == null || !TIME_PATTERN.matcher(start.trim()).matches() ||
                end == null || !TIME_PATTERN.matcher(end.trim()).matches()) {
            throw ApiException.badRequest("Date, start time and end time are required.");
        }
        if (timeIntervalService.toMin(end) <= timeIntervalService.toMin(start)) {
            throw ApiException.badRequest("End time must be after the start time.");
        }
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getFacultyAvailability(Long facultyId, String dateStr) {
        LocalDate date = parseAndValidateDate(dateStr);
        User faculty = userRepository.findById(facultyId)
                .filter(u -> u.getRole() == User.Role.FACULTY && u.isActive())
                .orElseThrow(() -> ApiException.notFound("Not found."));

        return calculateFacultyAvailability(faculty, date);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getRoomAvailability(Long roomId, String dateStr) {
        LocalDate date = parseAndValidateDate(dateStr);
        Room room = roomRepository.findById(roomId)
                .filter(Room::isActive)
                .orElseThrow(() -> ApiException.notFound("Not found."));

        return calculateRoomAvailability(room, date);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getResourceAvailability(Long resourceId, String dateStr) {
        LocalDate date = parseAndValidateDate(dateStr);
        Resource resource = resourceRepository.findById(resourceId)
                .filter(Resource::isActive)
                .orElseThrow(() -> ApiException.notFound("Not found."));

        return calculateResourceAvailability(resource, date);
    }

    @Transactional(readOnly = true)
    public boolean isFree(String kind, Long id, String dateStr, String start, String end) {
        AvailabilityResponse avail;
        if ("faculty".equalsIgnoreCase(kind)) {
            avail = getFacultyAvailability(id, dateStr);
        } else if ("room".equalsIgnoreCase(kind)) {
            avail = getRoomAvailability(id, dateStr);
        } else if ("resource".equalsIgnoreCase(kind)) {
            avail = getResourceAvailability(id, dateStr);
        } else {
            throw ApiException.badRequest("Invalid entity kind: " + kind);
        }

        List<TimeIntervalService.TimeRange> freeRanges = avail.getFree().stream()
                .map(r -> new TimeIntervalService.TimeRange(r.getStart(), r.getEnd()))
                .toList();

        return timeIntervalService.isFreeSlot(freeRanges, start, end);
    }

    @Transactional(readOnly = true)
    public CombinedAvailabilityResponse getCombinedAvailability(CombinedAvailabilityRequest req) {
        Long facultyId = req.getFacultyId();
        String dateFromStr = req.getDateFrom();
        String dateToStr = req.getDateTo();
        Integer duration = req.getDurationMinutes();

        if (facultyId == null || dateFromStr == null || dateToStr == null || duration == null || duration <= 0) {
            throw ApiException.badRequest("Faculty, date range and duration are required.");
        }

        LocalDate dateFrom = parseAndValidateDate(dateFromStr);
        LocalDate dateTo = parseAndValidateDate(dateToStr);

        long span = ChronoUnit.DAYS.between(dateFrom, dateTo);
        if (span < 0) {
            throw ApiException.badRequest("The end date must not be before the start date.");
        }
        if (span >= maxSearchDays) {
            throw ApiException.badRequest("Search at most " + maxSearchDays + " days at a time.");
        }

        User faculty = userRepository.findById(facultyId)
                .filter(u -> u.getRole() == User.Role.FACULTY && u.isActive())
                .orElseThrow(() -> ApiException.notFound("Faculty member not found."));

        boolean needRoom = Boolean.TRUE.equals(req.getNeedRoom());
        List<Room> rooms = new ArrayList<>();
        if (needRoom) {
            List<Room> allActiveRooms = roomRepository.findByActiveTrue();
            for (Room r : allActiveRooms) {
                if (req.getRoomId() != null && !r.getId().equals(req.getRoomId())) continue;
                if (req.getRoomType() != null && !req.getRoomType().isBlank() && !r.getType().name().equalsIgnoreCase(req.getRoomType())) continue;
                if (req.getBlock() != null && !req.getBlock().isBlank() && !matchesBlock(r, req.getBlock())) continue;
                if (req.getFloor() != null && !Objects.equals(r.getFloor(), req.getFloor())) continue;
                if (req.getMinCapacity() != null && r.getCapacity() < req.getMinCapacity()) continue;
                rooms.add(r);
            }
        }

        List<CombinedAvailabilityResponse.CombinedWindow> combinedWindows = new ArrayList<>();

        for (int i = 0; i <= span; i++) {
            LocalDate date = dateFrom.plusDays(i);
            AvailabilityResponse fAvail = calculateFacultyAvailability(faculty, date);
            List<TimeIntervalService.TimeRange> fFree = fAvail.getFree().stream()
                    .map(r -> new TimeIntervalService.TimeRange(r.getStart(), r.getEnd()))
                    .toList();

            String curDateStr = date.toString();

            if (!needRoom) {
                for (TimeIntervalService.TimeRange w : fFree) {
                    if (timeIntervalService.duration(w) >= duration) {
                        combinedWindows.add(new CombinedAvailabilityResponse.CombinedWindow(
                                curDateStr, w.start(), w.end(), null, null, null, null
                        ));
                    }
                }
                continue;
            }

            int roomsUsed = 0;
            for (Room room : rooms) {
                if (req.getRoomId() == null && req.getFloor() == null && roomsUsed >= MAX_ROOMS_PER_DAY) {
                    break;
                }

                AvailabilityResponse rAvail = calculateRoomAvailability(room, date);
                List<TimeIntervalService.TimeRange> rFree = rAvail.getFree().stream()
                        .map(r -> new TimeIntervalService.TimeRange(r.getStart(), r.getEnd()))
                        .toList();

                List<TimeIntervalService.TimeRange> intersected = timeIntervalService.intersect(fFree, rFree);
                List<TimeIntervalService.TimeRange> validWindows = intersected.stream()
                        .filter(w -> timeIntervalService.duration(w) >= duration)
                        .toList();

                if (!validWindows.isEmpty()) {
                    roomsUsed++;
                }

                for (TimeIntervalService.TimeRange w : validWindows) {
                    combinedWindows.add(new CombinedAvailabilityResponse.CombinedWindow(
                            curDateStr, w.start(), w.end(), room.getId(), room.getName(), room.getBuilding(), room.getCapacity()
                    ));
                }
            }
        }

        List<CombinedAvailabilityResponse.CombinedWindow> limited = combinedWindows.size() > 300
                ? combinedWindows.subList(0, 300)
                : combinedWindows;

        return new CombinedAvailabilityResponse(duration, limited);
    }

    // =========================================================================
    // Internal Availability Calculations
    // =========================================================================

    private AvailabilityResponse calculateFacultyAvailability(User faculty, LocalDate date) {
        LocalDate today = LocalDate.now();
        int dow = date.getDayOfWeek().getValue() % 7; // 0=Sunday, 1=Monday ... 6=Saturday

        List<TimeIntervalService.TimeRange> windows = new ArrayList<>();
        if (!date.isBefore(today)) {
            List<WorkingHour> whs = workingHourRepository.findByFacultyId(faculty.getId());
            for (WorkingHour wh : whs) {
                if (wh.getDayOfWeek() == dow) {
                    windows.add(new TimeIntervalService.TimeRange(wh.getStartTime(), wh.getEndTime()));
                }
            }
            if (date.isEqual(today)) {
                windows = trimPastTimes(windows);
            }
        }

        List<AvailabilityResponse.BusySlot> busy = new ArrayList<>();
        List<Appointment> appts = appointmentRepository.findActiveByFacultyAndDate(faculty.getId(), date);
        for (Appointment a : appts) {
            busy.add(new AvailabilityResponse.BusySlot(a.getStartTime(), a.getEndTime(), "Appointment"));
        }

        List<ScheduleBlock> blocks = scheduleBlockRepository.findByFacultyIdAndDate(faculty.getId(), date);
        for (ScheduleBlock b : blocks) {
            String label = (b.getReason() != null && !b.getReason().isBlank()) ? b.getReason() : "Blocked";
            busy.add(new AvailabilityResponse.BusySlot(b.getStartTime(), b.getEndTime(), label));
        }
        busy.sort(Comparator.comparing(AvailabilityResponse.BusySlot::getStart));

        List<TimeIntervalService.TimeRange> busyRanges = busy.stream()
                .map(b -> new TimeIntervalService.TimeRange(b.getStart(), b.getEnd()))
                .toList();

        List<TimeIntervalService.TimeRange> freeRanges = timeIntervalService.subtract(windows, busyRanges);

        return toAvailabilityResponse(date.toString(), windows, busy, freeRanges);
    }

    private AvailabilityResponse calculateRoomAvailability(Room room, LocalDate date) {
        LocalDate today = LocalDate.now();
        int dow = date.getDayOfWeek().getValue() % 7;

        List<TimeIntervalService.TimeRange> windows = new ArrayList<>();
        if (!date.isBefore(today) && dow != 0) {
            // Monday - Saturday: 08:00 - 20:00
            windows.add(new TimeIntervalService.TimeRange("08:00", "20:00"));
            if (date.isEqual(today)) {
                windows = trimPastTimes(windows);
            }
        }

        List<AvailabilityResponse.BusySlot> busy = new ArrayList<>();
        List<Appointment> appts = appointmentRepository.findActiveByRoomAndDate(room.getId(), date);
        for (Appointment a : appts) {
            busy.add(new AvailabilityResponse.BusySlot(a.getStartTime(), a.getEndTime(), "Faculty meeting"));
        }

        List<Booking> bookings = bookingRepository.findConfirmedByRoomAndDate(room.getId(), date);
        for (Booking b : bookings) {
            busy.add(new AvailabilityResponse.BusySlot(b.getStartTime(), b.getEndTime(), "Reserved"));
        }
        busy.sort(Comparator.comparing(AvailabilityResponse.BusySlot::getStart));

        List<TimeIntervalService.TimeRange> busyRanges = busy.stream()
                .map(b -> new TimeIntervalService.TimeRange(b.getStart(), b.getEnd()))
                .toList();

        List<TimeIntervalService.TimeRange> freeRanges = timeIntervalService.subtract(windows, busyRanges);

        return toAvailabilityResponse(date.toString(), windows, busy, freeRanges);
    }

    private AvailabilityResponse calculateResourceAvailability(Resource resource, LocalDate date) {
        LocalDate today = LocalDate.now();
        int dow = date.getDayOfWeek().getValue() % 7;

        List<TimeIntervalService.TimeRange> windows = new ArrayList<>();
        if (!date.isBefore(today) && dow != 0) {
            // Monday - Saturday: 09:00 - 18:00
            windows.add(new TimeIntervalService.TimeRange("09:00", "18:00"));
            if (date.isEqual(today)) {
                windows = trimPastTimes(windows);
            }
        }

        List<AvailabilityResponse.BusySlot> busy = new ArrayList<>();
        List<Booking> bookings = bookingRepository.findConfirmedByResourceAndDate(resource.getId(), date);
        for (Booking b : bookings) {
            busy.add(new AvailabilityResponse.BusySlot(b.getStartTime(), b.getEndTime(), "Reserved"));
        }
        busy.sort(Comparator.comparing(AvailabilityResponse.BusySlot::getStart));

        List<TimeIntervalService.TimeRange> busyRanges = busy.stream()
                .map(b -> new TimeIntervalService.TimeRange(b.getStart(), b.getEnd()))
                .toList();

        List<TimeIntervalService.TimeRange> freeRanges = timeIntervalService.subtract(windows, busyRanges);

        return toAvailabilityResponse(date.toString(), windows, busy, freeRanges);
    }

    private List<TimeIntervalService.TimeRange> trimPastTimes(List<TimeIntervalService.TimeRange> windows) {
        LocalTime now = LocalTime.now();
        int currentMinutes = now.getHour() * 60 + now.getMinute();
        int roundedMinutes = Math.min((int) Math.ceil(currentMinutes / 15.0) * 15, 1439);
        String cutoff = timeIntervalService.toTime(roundedMinutes);
        return timeIntervalService.subtract(windows, List.of(new TimeIntervalService.TimeRange("00:00", cutoff)));
    }

    private AvailabilityResponse toAvailabilityResponse(
            String dateStr,
            List<TimeIntervalService.TimeRange> windows,
            List<AvailabilityResponse.BusySlot> busy,
            List<TimeIntervalService.TimeRange> free) {
        List<AvailabilityResponse.TimeRange> winDto = windows.stream()
                .map(w -> new AvailabilityResponse.TimeRange(w.start(), w.end()))
                .toList();
        List<AvailabilityResponse.TimeRange> freeDto = free.stream()
                .map(f -> new AvailabilityResponse.TimeRange(f.start(), f.end()))
                .toList();

        return new AvailabilityResponse(dateStr, winDto, busy, freeDto);
    }

    private boolean matchesBlock(Room r, String block) {
        if (block == null || block.isBlank()) return true;
        String rBlock = (r.getBlock() != null && !r.getBlock().isBlank()) ? r.getBlock() : "OTHER";
        return rBlock.equalsIgnoreCase(block.trim());
    }
}
