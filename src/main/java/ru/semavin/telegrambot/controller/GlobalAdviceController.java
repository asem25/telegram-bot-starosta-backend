package ru.semavin.telegrambot.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.semavin.telegrambot.dto.ErrorResponseDTO;
import ru.semavin.telegrambot.utils.exceptions.ConnectFailedException;
import ru.semavin.telegrambot.utils.exceptions.GroupNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.InvalidFormatDateException;
import ru.semavin.telegrambot.utils.exceptions.ScheduleNotFoundException;

@ControllerAdvice
public class GlobalAdviceController {
    @ExceptionHandler(ScheduleNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleScheduleNotFoundException(ScheduleNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(
                        ErrorResponseDTO.builder()
                                .error(HttpStatus.NOT_FOUND.toString())
                                .error_description(ex.getMessage())
                                .build());

    }
    @ExceptionHandler(ConnectFailedException.class)
    public ResponseEntity<ErrorResponseDTO> handleConnectFailedException(ConnectFailedException ex) {
        return ResponseEntity.status(500)
                .body(
                        ErrorResponseDTO.builder()
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                                .error_description(ex.getMessage())
                                .build());

    }
    @ExceptionHandler(InvalidFormatDateException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidFormatDateException(InvalidFormatDateException ex) {
        return ResponseEntity.status(400)
                .body(
                        ErrorResponseDTO.builder()
                                .error(HttpStatus.BAD_REQUEST.toString())
                                .error_description(ex.getMessage())
                                .build());

    }
    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleGroupNotFoundException(GroupNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(
                        ErrorResponseDTO.builder()
                                .error(HttpStatus.NOT_FOUND.toString())
                                .error_description(ex.getMessage())
                                .build());

    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.builder()
                        .error(HttpStatus.NOT_FOUND.toString())
                        .error_description(ex.getMessage())
                        .build());
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.builder()
                        .error(HttpStatus.NOT_FOUND.toString())
                        .error_description(ex.getMessage())
                        .build());
    }
}
