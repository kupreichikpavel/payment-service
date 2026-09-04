package by.innowise.paymentservice.controller;

import by.innowise.paymentservice.dto.PaymentRequestDto;
import by.innowise.paymentservice.dto.PaymentResponseDto;
import by.innowise.paymentservice.entity.PaymentStatus;
import by.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDto create(
            @Valid @RequestBody PaymentRequestDto requestDto
    ) {
        return paymentService.create(requestDto);
    }

    @GetMapping("/user/{userId}")
    public List<PaymentResponseDto> getByUserId(
            @PathVariable Long userId
    ) {
        return paymentService.getByUserId(userId);
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentResponseDto> getByOrderId(
            @PathVariable Long orderId
    ) {
        return paymentService.getByOrderId(orderId);
    }

    @GetMapping("/status/{status}")
    public List<PaymentResponseDto> getByStatus(
            @PathVariable PaymentStatus status
    ) {
        return paymentService.getByStatus(status);
    }

    @GetMapping("/user/{userId}/total")
    public BigDecimal getUserTotal(
            @PathVariable Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        return paymentService.getTotalAmountByUserIdAndPeriod(
                userId,
                from,
                to
        );
    }

    @GetMapping("/total")
    public BigDecimal getTotal(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        return paymentService.getTotalAmountForPeriod(from, to);
    }
}