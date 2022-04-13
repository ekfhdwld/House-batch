package com.fastcampus.housebatch.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService implements SendService{
	
	private final JavaMailSender emailSender;
	
	@Override
	public void send(String email, String message) {
		final MimeMessagePreparator preparator = (MimeMessage mimeMessage) -> {
			final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
			helper.setTo(email);
			helper.setSubject("[아파트 실거래가 알림 메일]");
			helper.setText(message);
		};
		emailSender.send(preparator);
	}
}
