package com.stockpro.alert.service.impl;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AlertServiceImpl alertService;

    @Test
    void sendAlertSavesWarningWithoutEmail() {
        Alert alert = alert("WARNING");

        when(alertRepository.save(alert)).thenReturn(alert);

        Alert saved = alertService.sendAlert(alert);

        assertThat(saved).isSameAs(alert);
        verify(alertRepository).save(alert);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAlertSendsEmailForCriticalAlert() {
        Alert alert = alert("CRITICAL");
        alert.setTitle("Out of stock");
        alert.setMessage("Product is unavailable");

        when(alertRepository.save(alert)).thenReturn(alert);

        alertService.sendAlert(alert);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("admin@stockpro.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("[CRITICAL] Out of stock");
        assertThat(captor.getValue().getText()).isEqualTo("Product is unavailable");
    }

    @Test
    void sendLowStockAlertCreatesWarningWhenQuantityIsAboveZero() {
        when(alertRepository.existsUnacknowledgedLowStockAlert(11, 3)).thenReturn(false);
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertService.sendLowStockAlert(11, 3, 2);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("LOW_STOCK");
        assertThat(captor.getValue().getSeverity()).isEqualTo("WARNING");
        assertThat(captor.getValue().getChannel()).isEqualTo("IN_APP");
        assertThat(captor.getValue().getRelatedProductId()).isEqualTo(11);
        assertThat(captor.getValue().getRelatedWarehouseId()).isEqualTo(3);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendLowStockAlertCreatesCriticalAlertAndEmailWhenQuantityIsZero() {
        when(alertRepository.existsUnacknowledgedLowStockAlert(11, 3)).thenReturn(false);
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertService.sendLowStockAlert(11, 3, 0);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo("CRITICAL");
        assertThat(captor.getValue().getChannel()).isEqualTo("BOTH");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendLowStockAlertSkipsDuplicateUnacknowledgedAlert() {
        when(alertRepository.existsUnacknowledgedLowStockAlert(11, 3)).thenReturn(true);

        alertService.sendLowStockAlert(11, 3, 0);

        verify(alertRepository, never()).save(any(Alert.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendBulkAlertSavesOneAlertPerRecipient() {
        Alert template = new Alert();
        template.setType("SYSTEM");
        template.setSeverity("INFO");
        template.setTitle("Maintenance");
        template.setMessage("System window");
        template.setChannel("IN_APP");

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertService.sendBulkAlert(List.of(1, 2, 3), template);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Alert::getRecipientId)
                .containsExactly(1, 2, 3);
        assertThat(captor.getAllValues())
                .allSatisfy(alert -> {
                    assertThat(alert.getType()).isEqualTo("SYSTEM");
                    assertThat(alert.getSeverity()).isEqualTo("INFO");
                    assertThat(alert.getChannel()).isEqualTo("IN_APP");
                });
    }

    @Test
    void sendOverstockAlertCreatesWarningAlert() {
        when(alertRepository.existsUnacknowledgedOverstockAlert(11, 3)).thenReturn(false);
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertService.sendOverstockAlert(11, 3, 120, 100);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("OVERSTOCK");
        assertThat(captor.getValue().getSeverity()).isEqualTo("WARNING");
        assertThat(captor.getValue().getRelatedProductId()).isEqualTo(11);
        assertThat(captor.getValue().getRelatedWarehouseId()).isEqualTo(3);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void getByRecipientIncludesBroadcastAlerts() {
        List<Alert> expected = List.of(alert("INFO"));
        when(alertRepository.findByRecipientIdInOrderByCreatedAtDesc(List.of(5, 0))).thenReturn(expected);

        List<Alert> alerts = alertService.getByRecipient(5);

        assertThat(alerts).isSameAs(expected);
    }

    @Test
    void markAsReadSetsReadFlag() {
        Alert alert = alert("INFO");
        alert.setAlertId(7);

        when(alertRepository.findById(7)).thenReturn(Optional.of(alert));
        when(alertRepository.save(alert)).thenReturn(alert);

        Alert saved = alertService.markAsRead(7);

        assertThat(saved.isRead()).isTrue();
        verify(alertRepository).save(alert);
    }

    @Test
    void acknowledgeMarksReadAndAcknowledged() {
        Alert alert = alert("WARNING");
        alert.setAlertId(7);

        when(alertRepository.findById(7)).thenReturn(Optional.of(alert));
        when(alertRepository.save(alert)).thenReturn(alert);

        Alert saved = alertService.acknowledge(7);

        assertThat(saved.isRead()).isTrue();
        assertThat(saved.isAcknowledged()).isTrue();
        verify(alertRepository).save(alert);
    }

    @Test
    void deleteAlertDeletesWhenAlertExists() {
        when(alertRepository.existsById(7)).thenReturn(true);

        alertService.deleteAlert(7);

        verify(alertRepository).deleteById(7);
    }

    @Test
    void deleteAlertThrowsWhenAlertDoesNotExist() {
        when(alertRepository.existsById(7)).thenReturn(false);

        assertThatThrownBy(() -> alertService.deleteAlert(7))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("7");
        verify(alertRepository, never()).deleteById(7);
    }

    @Test
    void sendEmailDoesNotThrowWhenMailSenderFails() {
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        alertService.sendEmail("admin@stockpro.com", "Subject", "Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    private Alert alert(String severity) {
        Alert alert = new Alert();
        alert.setRecipientId(5);
        alert.setType("LOW_STOCK");
        alert.setSeverity(severity);
        alert.setTitle("Low Stock");
        alert.setMessage("Product is below reorder level");
        alert.setRelatedProductId(11);
        alert.setRelatedWarehouseId(3);
        alert.setChannel("IN_APP");
        return alert;
    }
}
