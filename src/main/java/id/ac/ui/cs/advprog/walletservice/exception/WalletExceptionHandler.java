package id.ac.ui.cs.advprog.walletservice.exception;

import id.ac.ui.cs.advprog.walletservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WalletExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return switch (exception.getMessage()) {
            case "Amount must be greater than zero" -> error(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", exception);
            case "Insufficient balance" -> error(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", exception);
            case "Insufficient held balance" -> error(HttpStatus.CONFLICT, "INSUFFICIENT_HELD_BALANCE", exception);
            case "Hold ownership mismatch" -> error(HttpStatus.FORBIDDEN, "HOLD_OWNERSHIP_MISMATCH", exception);
            case "Active hold not found for auction" -> error(HttpStatus.NOT_FOUND, "ACTIVE_HOLD_NOT_FOUND", exception);
            case "Hold amount mismatch" -> error(HttpStatus.CONFLICT, "HOLD_AMOUNT_MISMATCH", exception);
            default -> error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception);
        };
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, "INVALID_HOLD_STATE", exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        boolean hasAmountError = exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(fieldError -> "amount".equals(fieldError.getField()));
        if (hasAmountError) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_AMOUNT", "Amount must be greater than zero"));
        }
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, RuntimeException exception) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, exception.getMessage()));
    }
}
