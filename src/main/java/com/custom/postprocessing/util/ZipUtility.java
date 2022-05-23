package com.custom.postprocessing.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtility {
	private static final int BUFFER_SIZE = 4096;

	public void zipProcessing(List<String> listFiles, String destZipFile) throws FileNotFoundException, IOException {
		ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destZipFile));
		for (String fileName : listFiles) {
			File file = new File(fileName);
			if (file.isDirectory()) {
				zipDirectory(file, file.getName(), zipOutputStream);
			} else {
				zipFile(file, zipOutputStream);
			}
		}
		zipOutputStream.flush();
		zipOutputStream.close();
	}

	public void zipDirectory(File folder, String parentFolder, ZipOutputStream zipOutputStream)
			throws FileNotFoundException, IOException {
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				zipDirectory(file, parentFolder + "/" + file.getName(), zipOutputStream);
				continue;
			}
			FileInputStream fileInputStream = new FileInputStream(file);
			zipOutputStream.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			long bytesRead = 0;
			byte[] bytesIn = new byte[BUFFER_SIZE];
			int read = 0;
			while ((read = bufferedInputStream.read(bytesIn)) != -1) {
				zipOutputStream.write(bytesIn, 0, read);
				bytesRead += read;
			}
			fileInputStream.close();
			zipOutputStream.closeEntry();
			bufferedInputStream.close();
		}
	}

	private void zipFile(File file, ZipOutputStream zipOutputStream) throws FileNotFoundException, IOException {
		zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
		FileInputStream fileInputStream = new FileInputStream(file);
		BufferedInputStream bufferInputStream = new BufferedInputStream(fileInputStream);
		long bytesRead = 0;
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = bufferInputStream.read(bytesIn)) != -1) {
			zipOutputStream.write(bytesIn, 0, read);
			bytesRead += read;
		}
		fileInputStream.close();
		bufferInputStream.close();
		zipOutputStream.closeEntry();
	}
}