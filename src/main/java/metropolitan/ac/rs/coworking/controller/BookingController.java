package metropolitan.ac.rs.coworking.controller;

import lombok.RequiredArgsConstructor;
import metropolitan.ac.rs.coworking.model.Booking;
import metropolitan.ac.rs.coworking.model.BookingStatus;
import metropolitan.ac.rs.coworking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings(@RequestParam(required = false)BookingStatus status){
        return ResponseEntity.ok(bookingService.getAllBookings(status));
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody Booking booking, Principal principal) {
        Booking created = bookingService.createBooking(booking, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam BookingStatus status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status));
    }
}
