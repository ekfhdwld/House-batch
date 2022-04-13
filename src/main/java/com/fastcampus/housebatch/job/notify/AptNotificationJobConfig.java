package com.fastcampus.housebatch.job.notify;

import com.fastcampus.housebatch.core.service.EmailService;
import com.fastcampus.housebatch.core.service.FakeSendService;
import com.fastcampus.housebatch.core.dto.AptDto;
import com.fastcampus.housebatch.core.dto.NotificationDto;
import com.fastcampus.housebatch.core.entity.AptNotification;
import com.fastcampus.housebatch.core.repository.AptNotificationRepository;
import com.fastcampus.housebatch.core.repository.LawdRepository;
import com.fastcampus.housebatch.core.service.AptDealService;
import com.fastcampus.housebatch.job.validator.DealDataParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptNotificationJobConfig {
	
	private final JobBuilderFactory jobBuilderFactory;
	private final StepBuilderFactory stepBuilderFactory;
	
	@Bean
	public Job aptNotificationJob(Step aptNotificationStep) {
		return jobBuilderFactory.get("aptNotificationJob")
		  .incrementer(new RunIdIncrementer())
		  .validator(new DealDataParameterValidator())
		  .start(aptNotificationStep)
		  .build();
	}
	
	@JobScope
	@Bean
	public Step aptNotificationStep(RepositoryItemReader<AptNotification> aptNotificationRepositoryItemReader,
	                                ItemProcessor<AptNotification, NotificationDto> aptNotificationProcessor,
	                                ItemWriter<NotificationDto> aptNotificationWriter) {
		return stepBuilderFactory.get("aptNotificationStep")
		  .<AptNotification, NotificationDto>chunk(20)
		  .reader(aptNotificationRepositoryItemReader)
		  .processor(aptNotificationProcessor)
		  .writer(aptNotificationWriter)
		  .build();
	}
	
	@StepScope
	@Bean
	public RepositoryItemReader<AptNotification> aptNotificationRepositoryItemReader(
	  AptNotificationRepository aptNotificationRepository
	) {
		return new RepositoryItemReaderBuilder<AptNotification>()
		  .name("aptNotificationRepositoryItemReader")
		  .repository(aptNotificationRepository)
		  .methodName("findByEnabledIsTrue")
		  .pageSize(10)
		  .arguments(Arrays.asList())
		  .sorts(Collections.singletonMap("aptNotificationId", Sort.Direction.DESC))
		  .build();
	}
	
	@StepScope
	@Bean
	public ItemProcessor<AptNotification, NotificationDto> aptNotificationProcessor(
	    @Value("#{jobParameters['dealDate']}") String dealDate,
	    AptDealService aptDealService,
	    LawdRepository lawdRepository
	    
	) {
		return aptNotification -> {
			List<AptDto> aptDtoList = aptDealService.findByGuLawdCdAndDealDate(aptNotification.getGuLawdCd(), LocalDate.parse(dealDate));
			
			if(aptDtoList.isEmpty()){
				return null;
			}
			
			String guName = lawdRepository.findByLawdCd(aptNotification.getGuLawdCd() + "00000")
						  .orElseThrow().getLawdDong();
			  
			return NotificationDto.builder()
			  .email(aptNotification.getEmail())
			  .guName(guName)
			  .count(aptDtoList.size())
			  .aptDeals(aptDtoList)
			  .build();
		};
	}
	
	@StepScope
	@Bean
	public ItemWriter<NotificationDto> aptNotificationWriter(EmailService emailService) {
		return items -> items.forEach(item -> emailService.send(item.getEmail(), item.toMessage()));
	}
}
