package com.custom.postprocessing.email.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kumar.charanswain
 *
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailResponseDTO {
	
	private String fileType;
	private int totalSize;
	
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public int getTotalSize() {
		return totalSize;
	}
	public void setTotalSize(int totalSize) {
		this.totalSize = totalSize;
	}
}
