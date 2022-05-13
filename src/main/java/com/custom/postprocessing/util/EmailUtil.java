package com.custom.postprocessing.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.custom.postprocessing.email.api.dto.MailRequest;
import com.custom.postprocessing.email.api.dto.MailResponse;
import com.custom.postprocessing.email.api.dto.MailResponseDTO;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * @author kumar.charanswain
 *
 */

@Component
public class EmailUtil {

	Logger logger = LoggerFactory.getLogger(EmailUtil.class);

	@Autowired
	private JavaMailSender sender;

	@Autowired
	private Configuration config;

	@Value("${mail.form}")
	private String mailForm;

	@Value("${mail.to}")
	private String mailTo;

	@Value("${mail.subject}")
	private String subject;

	public MailResponse sendEmail(MailRequest request, Map<String, Object> model, String currentDate) {
		MailResponse response = new MailResponse();
		MimeMessage message = sender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
			File documentTxtFile = addAttachment(currentDate, request.getFileNames());
			helper.addAttachment(documentTxtFile.toString(), documentTxtFile);
			Template t = config.getTemplate("email-template.ftl");
			String html = FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
			helper.setTo(request.getTo());
			helper.setText(html, true);
			helper.setSubject(request.getSubject());
			helper.setFrom(request.getFrom());
			sender.send(message);

			response.setMessage("mail send to : " + request.getTo());
			response.setStatus(Boolean.TRUE);
			documentTxtFile.delete();
		} catch (Exception exception) {
			logger.info("exception:");
		}

		return response;
	}

	public File addAttachment(String currentDate, List<String> fileNames) {
		File file = null;
		try {
			String documentFileName = "completed-" + currentDate + ".txt";
			file = new File(documentFileName);
			FileOutputStream outputStream = new FileOutputStream(file);
			PrintWriter writer = new PrintWriter(outputStream);
			writer.println("process file type summary" + '\n');
			for (String fileName : fileNames) {
				writer.println(fileName);
			}
			writer.close();
			return file;
		} catch (Exception exception) {
			logger.info("Exception:" + exception.getMessage());
		}
		return file;
	}

	public void emailProcess(ConcurrentHashMap<String, List<String>> updatePostProcessMap, String currentDate) {

		List<String> updateFileNames = new LinkedList<String>();
		List<MailResponseDTO> mailResponseDTOList = new LinkedList<MailResponseDTO>();
		for (String fileType : updatePostProcessMap.keySet()) {
			List<String> fileNames = updatePostProcessMap.get(fileType);
			addFileNameList(fileNames, updateFileNames);

			MailResponseDTO mailResponseDTO = new MailResponseDTO();
			mailResponseDTO.setFileType(fileType);
			mailResponseDTO.setTotalSize(fileNames.size());
			mailResponseDTOList.add(mailResponseDTO);
		}

		MailRequest mailRequest = new MailRequest();
		mailRequest.setFrom(mailForm);
		mailRequest.setTo(mailTo);
		mailRequest.setSubject(subject);
		mailRequest.setFileNames(updateFileNames);

		Map<String, Object> model = new HashMap<>();
		model.put("mailResponseList", mailResponseDTOList);
		sendEmail(mailRequest, model, currentDate);
	}

	public void addFileNameList(List<String> fileNames, List<String> updateFileNames) {
		for (String fileName : fileNames) {
			updateFileNames.add(fileName);
		}
	}

}
