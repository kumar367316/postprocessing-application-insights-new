package com.custom.postprocessing.email.api.dto;

import java.util.List;

import lombok.Data;

/**
 * @author kumar.charanswain
 *
 */

@Data
public class MailRequest {

	private String to;
	private String from;
	private String subject;
	private List<String> fileNames;

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public List<String> getFileNames() {
		return fileNames;
	}

	public void setFileNames(List<String> fileNames) {
		this.fileNames = fileNames;
	}
}
