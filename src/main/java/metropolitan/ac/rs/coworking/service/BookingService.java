package metropolitan.ac.rs.coworking.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import metropolitan.ac.rs.coworking.exception.ResourceNotFoundException;
import metropolitan.ac.rs.coworking.model.Booking;
import metropolitan.ac.rs.coworking.model.BookingStatus;
import metropolitan.ac.rs.coworking.model.User;
import metropolitan.ac.rs.coworking.model.Workspace;
import metropolitan.ac.rs.coworking.repository.BookingRepository;
import metropolitan.ac.rs.coworking.repository.UserRepository;
import metropolitan.ac.rs.coworking.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {
    private final BookingRepository bookingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public List<Booking> getAllBookings(BookingStatus status) {
        if (status != null) {
            return bookingRepository.findByStatus(status);
        }
        return bookingRepository.findAll();
    }

    public Booking createBooking(Booking booking, String username) {
        if (booking.getWorkspace() == null || booking.getWorkspace().getId() == null) {
            throw new IllegalArgumentException("Morate navesti ID radne jedinice.");
        }
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            throw new IllegalArgumentException("Vreme pocetka i vreme zavrsetka su obavezni.");
        }
        Workspace workspace = workspaceRepository.findById(booking.getWorkspace().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Radna jedinica ne postoji."));
        User member = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik nije pronadjen."));
        LocalDateTime start = booking.getStartTime();
        LocalDateTime end = booking.getEndTime();

        if(start.isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException("Ne mozete rezervisati termin u proslosti.");
        }

        if (end.isBefore(start) || end.isEqual(start)) {
            throw new IllegalArgumentException("Vreme zavrsetka mora biti nakon vremena pocetka.");
        }

        long minutes = Duration.between(start, end).toMinutes();
        if(minutes > 8 * 60) {
            throw new IllegalArgumentException("Maksimalno trajanje jedne rezervacije je 8 sati.");
        }

        double hours = (double) minutes / 60.0;
        booking.setTotalPrice(hours * workspace.getHourlyRate());

        List<Booking> overlappingApproved = bookingRepository
                .findOverlappingApprovedBookings(workspace.getId(), start, end, BookingStatus.APPROVED);
        if(!overlappingApproved.isEmpty()) {
            throw new IllegalArgumentException("Izabrana radna jedinica je vec zauzeta u ovom terminu.");
        }

        booking.setWorkspace(workspace);
        booking.setMember(member);
        booking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }
    public Booking updateBookingStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervacija nije pronadjena"));
        if (newStatus == BookingStatus.APPROVED) {
            booking.setStatus(BookingStatus.APPROVED);

            List<Booking> overlappingPending = bookingRepository.findOverlappingPendingBookings(
                    booking.getWorkspace().getId(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getId(),
                    BookingStatus.PENDING
            );

            for (Booking pending : overlappingPending) {
                pending.setStatus(BookingStatus.REJECTED);
                bookingRepository.save(pending);
            }
        } else {
            booking.setStatus(newStatus);
        }
        return bookingRepository.save(booking);
    }
}
