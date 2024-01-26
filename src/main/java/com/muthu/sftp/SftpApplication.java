package com.muthu.sftp;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

@SpringBootApplication
@IntegrationComponentScan
@EnableIntegration
public class SftpApplication {

	// Properties of Remote Host
	@Value("${sftp.host.ip}")
	private String sftpHostIp;

	@Value("${sftp.host.port}")
	private int sftphostPort;

	@Value("${sftp.host.user}")
	private String sftpHostUser;

	@Value("${sftp.host.password}")
	private String sftpHostPassword;

	@Value("${sftp.host.remote.directory.download}")
	private String sftpRemoteDirectoryDownloadHost;

	public static void main(String[] args) {
		new SpringApplicationBuilder(SftpApplication.class).run(args);
	}

	@Bean
	public DefaultSftpSessionFactory sftpSessionFactoryRemoteHost() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		factory.setHost(sftpHostIp);
		factory.setPort(sftphostPort);
		factory.setUser(sftpHostUser);
		factory.setPassword(sftpHostPassword);
		factory.setAllowUnknownKeys(true);
		System.out.println("--> Session factory created -->");
		return factory;
	}
	
	@Bean
    public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
        SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactoryRemoteHost());
        fileSynchronizer.setDeleteRemoteFiles(false);
		fileSynchronizer.setRemoteDirectory("/muthu/king");
		fileSynchronizer.setPreserveTimestamp(true);
		fileSynchronizer.setFilter(new SftpSimplePatternFileListFilter("*"));
		fileSynchronizer.synchronizeToLocalDirectory(new File("sftp-inbound"));
        return fileSynchronizer;
	}
	
	@Bean
    @InboundChannelAdapter(channel = "sftpChannel", poller = @Poller(fixedDelay = "5000"))
    public MessageSource<File> sftpMessageSource() {
		SftpInboundFileSynchronizingMessageSource source = new SftpInboundFileSynchronizingMessageSource(
				sftpInboundFileSynchronizer());		
		source.setLocalDirectory(new File("D://StudyMaterials//FileTransferApp//files"));
		source.setAutoCreateLocalDirectory(true);
		source.setLocalFilter(new AcceptOnceFileListFilter<File>());
		 System.out.println("--> sftpChannel inbound -->");
		return source;
	}

	@Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler handler() {
        return new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				 System.out.println("hello");
				 System.out.println(message.getPayload());
				
			}

        };
    }


	/*@Bean
	public IntegrationFlow sftpInboundFlow() {
		return IntegrationFlow
				.from(Sftp.inboundAdapter(this.sftpSessionFactoryRemoteHost()).preserveTimestamp(true)
						.remoteDirectory("/muthu/king")
						// .regexFilter(".*\\.txt$")
						// .localFilenameExpression("#this.toUpperCase() + '.a'")
						.localDirectory(new File("sftp-inbound")),
						e -> e.id("sftpInboundAdapter").autoStartup(true).poller(Pollers.fixedDelay(5000)))
				.handle(m -> System.out.println(m.getPayload())).get();
	}*/

}
