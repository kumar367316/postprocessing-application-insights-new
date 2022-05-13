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
public class MailResponse {
	private String message;
	private boolean status;

}
