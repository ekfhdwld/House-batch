package com.fastcampus.housebatch.job.apt;

import com.fastcampus.housebatch.adapter.ApartmentApiResource;
import com.fastcampus.housebatch.core.dto.AptDealDto;
import com.fastcampus.housebatch.job.validator.LawdCdParameterValidator;
import com.fastcampus.housebatch.job.validator.YearMonthParameterValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.time.YearMonth;
import java.util.Arrays;

@Configuration
@AllArgsConstructor
@Slf4j
public class AptDealInsertJobConfig {
	
	private JobBuilderFactory jobBuilderFactory;
	private StepBuilderFactory stepBuilderFactory;
	
	private ApartmentApiResource apartmentApiResource;
	
	@Bean
	public Job aptDealInsertJob(Step aptDealInsertStep){
		return jobBuilderFactory.get("aptDealInsertJob")
		  .incrementer(new RunIdIncrementer())
		  .validator(aptDealJobParameterValidator())
		  .start(aptDealInsertStep)
		  .build();
	}
	
	private JobParametersValidator aptDealJobParameterValidator(){
		CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
		validator.setValidators(Arrays.asList(
		  new YearMonthParameterValidator(),
		  new LawdCdParameterValidator()
		));
		return validator;
	}
	
	
	@Bean
	@JobScope
	public Step aptDealInsertStep(StaxEventItemReader<AptDealDto> aptDealResourceReader,
	                              ItemWriter<AptDealDto> aptDealWriter){
		return stepBuilderFactory.get("aptDealInsertStep")
		  .<AptDealDto, AptDealDto>chunk(10)
		  .reader(aptDealResourceReader)
		  .writer(aptDealWriter)
		  .build();
	}
	
	@Bean
	@StepScope
	public StaxEventItemReader<AptDealDto> aptDealResourceReader(
	    @Value("#{jobParameters['yearMonth']}") String yearMonth,
	    @Value("#{jobParameters['lawdCd']}") String lawdCd,
	    Jaxb2Marshaller aptDealDtoMarshaller
	){
		return new StaxEventItemReaderBuilder<AptDealDto>()
		  .name("aptDealResourceReader")
		  .resource(apartmentApiResource.getResource(lawdCd, YearMonth.parse(yearMonth)))
		  .addFragmentRootElements("item")
		  .unmarshaller(aptDealDtoMarshaller)
		  .build();
	}
	
	@Bean
	@StepScope
	public Jaxb2Marshaller aptDealDtoMarshaller(){
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(AptDealDto.class);
		return jaxb2Marshaller;
	}
	
	@Bean
	@StepScope
	public ItemWriter<AptDealDto> aptDealWriter(){
		return items -> {
			items.forEach(System.out::println);
		};
	}
}
