package ru.semavin.telegrambot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.semavin.telegrambot.dto.ErrorResponseDTO;
import ru.semavin.telegrambot.utils.exceptions.*;

import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalAdviceController {
    @ExceptionHandler(ScheduleNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleScheduleNotFoundException(ScheduleNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(
                        ErrorResponseDTO.builder()
                                .error(String.valueOf(HttpStatus.NOT_FOUND.value()))
                                .error_description(ex.getMessage())
                                .build());

    }
    @ExceptionHandler(ConnectFailedException.class)
    public ResponseEntity<ErrorResponseDTO> handleConnectFailedException(ConnectFailedException ex) {
        return ResponseEntity.status(500)
                .body(
                        ErrorResponseDTO.builder()
                                .error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                                .error_description(ex.getMessage())
                                .build());

    }
    @ExceptionHandler(InvalidFormatDateException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidFormatDateException(InvalidFormatDateException ex) {
        return ResponseEntity.status(400)
                .body(
                        ErrorResponseDTO.builder()
                                .error(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                                .error_description(ex.getMessage())
                                .build());

    }
    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleGroupNotFoundException(GroupNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(
                        ErrorResponseDTO.builder()
                                .error(String.valueOf(HttpStatus.NOT_FOUND.value()))
                                .error_description(ex.getMessage())
                                .build());

    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(MethodArgumentNotValidException ex) {
        Set<String> fieldErrors = ex.getFieldErrors().stream()
                .map(error -> String.format("Поле %s:%s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toSet());



        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.builder()
                        .error(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .error_description(fieldErrors.toString())
                        .build());
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.builder()
                        .error(String.valueOf(HttpStatus.NOT_FOUND.value()))
                        .error_description(ex.getMessage())
                        .build());
    }
    @ExceptionHandler(KeyNotEqualsException.class)
    public ResponseEntity<ErrorResponseDTO> handleKeyNotEqualsException(KeyNotEqualsException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDTO.builder()
                        .error(String.valueOf(HttpStatus.FORBIDDEN.value()))
                        .error_description(ex.getMessage())
                        .build());
    }
    @ExceptionHandler(UserAlreadyExistsForStarostaException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserAlreadyExistsForStarostaException(UserAlreadyExistsForStarostaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.builder()
                        .error(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .error_description(ex.getMessage())
                        .build());
    }
    @ExceptionHandler(UserWithTelegramIdAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserWithTelegramIdAlreadyExistsException(UserWithTelegramIdAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.builder()
                        .error(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .error_description(ex.getMessage())
                        .build());
    }
}
