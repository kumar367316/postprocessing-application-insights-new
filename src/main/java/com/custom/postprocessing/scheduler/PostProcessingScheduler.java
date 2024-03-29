package com.custom.postprocessing.scheduler;

import static com.custom.postprocessing.constant.PostProcessingConstant.ARCHIVE_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.ARCHIVE_VALUE;
import static com.custom.postprocessing.constant.PostProcessingConstant.BACKSLASH_ASCII;
import static com.custom.postprocessing.constant.PostProcessingConstant.BANNER_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.BANNER_PAGE;
import static com.custom.postprocessing.constant.PostProcessingConstant.EMPTY_SPACE;
import static com.custom.postprocessing.constant.PostProcessingConstant.FILE_SEPARATION;
import static com.custom.postprocessing.constant.PostProcessingConstant.LICENSE_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.LICENSE_FILE_NAME;
import static com.custom.postprocessing.constant.PostProcessingConstant.LOG_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.LOG_FILE;
import static com.custom.postprocessing.constant.PostProcessingConstant.PCL_EXTENSION;
import static com.custom.postprocessing.constant.PostProcessingConstant.PDF_EXTENSION;
import static com.custom.postprocessing.constant.PostProcessingConstant.POSTPROCESSING_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.PRINT_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.PROCESSED_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.ROOT_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.SPACE_VALUE;
import static com.custom.postprocessing.constant.PostProcessingConstant.TRANSIT_DIRECTORY;
import static com.custom.postprocessing.constant.PostProcessingConstant.XML_EXTENSION;
import static com.custom.postprocessing.constant.PostProcessingConstant.XML_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.aspose.pdf.License;
import com.aspose.pdf.facades.PdfFileEditor;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.custom.postprocessing.constant.PostProcessingConstant;
import com.custom.postprocessing.util.EmailUtil;
import com.custom.postprocessing.util.PostProcessUtil;
import com.custom.postprocessing.util.ZipUtility;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

/**
 * @author kumar.charanswain
 *
 */

@Service
public class PostProcessingScheduler {

	public static Logger logger = LoggerFactory.getLogger(PostProcessingScheduler.class);

	@Value("${blob.account.name.key}")
	private String connectionNameKey;

	@Value("${blob.container.name}")
	private String containerName;

	@Value("#{'${state.allow.type}'.split(',')}")
	private List<String> stateAllowType;

	@Value("#{'${page.type}'.split(',')}")
	private List<String> pageTypeList;

	@Value("${sheet.number.type}")
	private String sheetNbrType;

	@Autowired
	EmailUtil emailUtil;

	@Autowired
	private PostProcessUtil postProcessUtil;

	List<String> pclFileList = new LinkedList<>();

	@Scheduled(cron = PostProcessingConstant.CRONJOB_ARCHIVEINTERVAL)
	public void postProcessing() {
		logger.info("continues deployment:start postProcessing batch DevOps pipeline deployment testing");
		String message = smartComPostProcessing();
		logger.info(message);
	}

	@Scheduled(cron = PostProcessingConstant.CRONJOB_INTERVAL)
	public void archivedPostProcessingScheduler() {
		archivePostProcessing();
	}

	public String smartComPostProcessing() {
		String messageInfo = "smart comm post processing successfully";
		String currentDate = currentDateTime();
		try {
			CloudBlobContainer container = containerInfo();
			CloudBlobDirectory transitDirectory = getDirectoryName(container, POSTPROCESSING_DIRECTORY + TRANSIT_DIRECTORY,
					currentDate + "-" + PRINT_DIRECTORY);
			String transitTargetDirectory = POSTPROCESSING_DIRECTORY + TRANSIT_DIRECTORY + "/" + currentDate + "-";
			if (moveFileToTargetDirectory(PRINT_DIRECTORY, transitTargetDirectory)) {
				messageInfo = processMetaDataInputFile(transitDirectory, currentDate);
				String logFile = LOG_FILE + currentDate + ".log";
				logger.info("logFile file:" + logFile);
				copyFileToTargetDirectory(logFile, "postprocessing", LOG_DIRECTORY);
				deletePreviousLogFile();
			} else {
				messageInfo = "no file for post processing";
			}
		} catch (Exception exception) {
			logger.info("Exception:" + exception.getMessage());
			messageInfo = "error in copy file to blob directory";
		}
		return messageInfo;
	}

	public void archivePostProcessing() {
		String message = "post processing archive completed successfully";
		try {
			String currentDate = currentDateTime();
			String targetDirectory = POSTPROCESSING_DIRECTORY + TRANSIT_DIRECTORY + "/" + currentDate + "-"
					+ ARCHIVE_DIRECTORY;
			message = zipFileTransferToArchive(currentDate, ARCHIVE_DIRECTORY, targetDirectory);
		} catch (Exception exception) {
			message = "error in post processing archive";
			logger.info("error in archive file:" + exception.getMessage());
		}
		logger.info(message);
	}

	private boolean moveFileToTargetDirectory(String sourceDirectory, String targetDirectory) {
		boolean moveSuccess = false;
		BlobContainerClient blobContainerClient = getBlobContainerClient(connectionNameKey, containerName);
		Iterable<BlobItem> listBlobs = blobContainerClient.listBlobsByHierarchy(sourceDirectory);
		for (BlobItem blobItem : listBlobs) {
			BlobClient dstBlobClient = blobContainerClient.getBlobClient(targetDirectory + blobItem.getName());
			BlobClient srcBlobClient = blobContainerClient.getBlobClient(blobItem.getName());
			String updateSrcUrl = srcBlobClient.getBlobUrl();
			if (srcBlobClient.getBlobUrl().contains(BACKSLASH_ASCII)) {
				updateSrcUrl = srcBlobClient.getBlobUrl().replace(BACKSLASH_ASCII, FILE_SEPARATION);
			}
			dstBlobClient.copyFromUrl(updateSrcUrl);
			dstBlobClient.beginCopy(updateSrcUrl, null);
			srcBlobClient.delete();
			moveSuccess = true;
		}
		return moveSuccess;
	}

	public String processMetaDataInputFile(CloudBlobDirectory transitDirectory, String currentDate) {
		ConcurrentHashMap<String, List<String>> postProcessMap = new ConcurrentHashMap<>();
		String message = "smart comm post processing successfully";
		try {
			Iterable<ListBlobItem> blobList = transitDirectory.listBlobs();

			for (ListBlobItem blobItem : blobList) {
				String fileName = getFileNameFromBlobURI(blobItem.getUri()).replace(SPACE_VALUE, EMPTY_SPACE);
				boolean stateType = checkStateType(fileName);
				if (stateType) {
					if (StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(fileName), XML_TYPE)) {
						continue;
					}
					String fileNameNoExt = FilenameUtils.removeExtension(fileName);
					String[] stateAndSheetNameList = StringUtils.split(fileNameNoExt, "_");
					String stateAndSheetName = stateAndSheetNameList.length > 0
							? stateAndSheetNameList[stateAndSheetNameList.length - 1]
							: "";
					prepareMap(postProcessMap, stateAndSheetName, fileName);
				} else if (checkPageType(fileName)) {
					if (PostProcessingConstant.PDF_TYPE.equals(FilenameUtils.getExtension(fileName))) {
						continue;
					}
					prepareMap(postProcessMap, getSheetNumber(fileName, blobItem),
							StringUtils.replace(fileName, XML_EXTENSION, PDF_EXTENSION));
				} else {
					logger.info("unable to process:invalid document type ");
				}
			}
			if (postProcessMap.size() > 0) {
				message = mergePDF(postProcessMap, currentDate);
			} else {
				message = "unable to process :invalid state/document name";
			}
		} catch (Exception exception) {
			logger.info("Exception found:" + exception.getMessage());
		}
		return message;
	}

	private String getSheetNumber(String fileName, ListBlobItem blobItem) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			File file = new File(fileName);
			CloudBlob cloudBlob = (CloudBlob) blobItem;
			cloudBlob.downloadToFile(file.getPath());
			Document document = builder.parse(file);
			document.getDocumentElement().normalize();
			Element root = document.getDocumentElement();
			int sheetNumber = Integer.parseInt(root.getAttribute(sheetNbrType));
			if (sheetNumber <= 10) {
				file.delete();
				return String.valueOf(sheetNumber);
			}
			file.delete();
		} catch (Exception exception) {
			logger.info("Exception found:" + exception.getMessage());
		}
		return PostProcessingConstant.MULTIPAGE;
	}

	public BlobContainerClient getBlobContainerClient(String connectionNameKey, String containerName) {
		BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionNameKey)
				.buildClient();
		return blobServiceClient.getBlobContainerClient(containerName);
	}

	// post merge PDF
	public String mergePDF(ConcurrentHashMap<String, List<String>> postProcessMap, String currentDate)
			throws IOException {
		String message = "smart comm post processing successfully";
		ConcurrentHashMap<String, List<String>> updatePostProcessMap = new ConcurrentHashMap<>();
		List<String> fileNameList = new LinkedList<>();
		CloudBlobContainer container = containerInfo();
		for (String fileType : postProcessMap.keySet()) {
			try {
				PDFMergerUtility PDFMerger = new PDFMergerUtility();
				fileNameList = postProcessMap.get(fileType);
				String bannerFileName = getBannerPage(fileType);
				File bannerFile = new File(bannerFileName);
				PDFMerger.addSource(bannerFileName);
				Collections.sort(fileNameList);
				CloudBlobDirectory transitDirectory = getDirectoryName(container,
						POSTPROCESSING_DIRECTORY + TRANSIT_DIRECTORY + "/", currentDate + "-" + PRINT_DIRECTORY);
				for (String fileName : fileNameList) {
					logger.info("process file details:" + fileName);
					File file = new File(fileName);
					CloudBlockBlob blob = transitDirectory.getBlockBlobReference(fileName);
					blob.downloadToFile(file.getAbsolutePath());
					PDFMerger.addSource(file.getPath());
				}
				fileType = postProcessUtil.getFileType(fileType);
				String mergePdfFile = fileType + "-merge" + "-" + currentDate + PDF_EXTENSION;
				PDFMerger.setDestinationFileName(mergePdfFile);
				PDFMerger.mergeDocuments();
				convertPDFToPCL(mergePdfFile, container);
				updatePostProcessMap.put(fileType, fileNameList);
				bannerFile.delete();
				new File(mergePdfFile).delete();
				deleteFiles(fileNameList);
			} catch (StorageException storageException) {
				logger.info("file not found for processing");
				if (fileNameList.size() > 0) {
					deleteFiles(fileNameList);
				}
				continue;
			} catch (Exception exception) {
				logger.info("Exception:" + exception.getMessage());
			}
		}
		if (postProcessMap.size() > 0) {
			emailUtil.emailProcess(updatePostProcessMap, currentDate);
		}
		File licenseFile = new File(LICENSE_FILE_NAME);
		licenseFile.delete();
		return message;
	}

	// post processing PDF to PCL conversion
	public void convertPDFToPCL(String mergePdfFile, CloudBlobContainer container) throws IOException {
		try {
			String outputPclFile = FilenameUtils.removeExtension(mergePdfFile) + PCL_EXTENSION;
			CloudBlobDirectory transitDirectory = getDirectoryName(container, ROOT_DIRECTORY, LICENSE_DIRECTORY);
			CloudBlockBlob blob = transitDirectory.getBlockBlobReference(LICENSE_FILE_NAME);
			String licenseFiles[] = blob.getName().split("/");
			String licenseFileName = licenseFiles[licenseFiles.length - 1];
			blob.downloadToFile(new File(licenseFileName).getAbsolutePath());
			License license = new License();
			license.setLicense(licenseFileName);
			PdfFileEditor fileEditor = new PdfFileEditor();
			InputStream stream = new FileInputStream(mergePdfFile);
			InputStream[] streamList = new InputStream[] { stream };
			OutputStream outStream = new FileOutputStream(outputPclFile);
			fileEditor.concatenate(streamList, outStream);
			stream.close();
			outStream.close();
			fileEditor.setCloseConcatenatedStreams(true);
			copyFileToTargetDirectory(outputPclFile, POSTPROCESSING_DIRECTORY + TRANSIT_DIRECTORY, PROCESSED_DIRECTORY);
			pclFileList.add(outputPclFile);
			new File(outputPclFile).delete();
		} catch (Exception exception) {
			logger.info("Exception:" + exception.getMessage());
		}
	}

	public void copyFileToTargetDirectory(String fileName, String rootDirectory, String targetDirectory) {
		try {
			CloudBlobContainer container = containerInfo();
			CloudBlobDirectory processDirectory = getDirectoryName(container, rootDirectory, targetDirectory);
			File outputFileName = new File(fileName);
			CloudBlockBlob processSubDirectoryBlob = processDirectory.getBlockBlobReference(fileName);
			FileInputStream fileInputStream = new FileInputStream(outputFileName);
			processSubDirectoryBlob.upload(fileInputStream, outputFileName.length());
			fileInputStream.close();
		} catch (Exception exception) {
			logger.info("Exception:" + exception.getMessage());
		}
	}

	public boolean checkStateType(String fileName) {
		for (String state : stateAllowType) {
			if (fileName.contains(state)) {
				return true;
			}
		}
		return false;
	}

	public boolean checkPageType(String fileName) {
		for (String pageType : pageTypeList) {
			if (fileName.contains(pageType)) {
				return true;
			}
		}
		return false;
	}

	public void deleteFiles(List<String> fileNameList) {
		for (String fileName : fileNameList) {
			File file = new File(fileName);
			file.delete();
		}
	}

	public void prepareMap(ConcurrentHashMap<String, List<String>> postProcessMap, String key, String fileName) {
		if (postProcessMap.containsKey(key)) {
			List<String> existingFileNameList = postProcessMap.get(key);
			existingFileNameList.add(fileName);
			postProcessMap.put(key, existingFileNameList);
		} else {
			List<String> existingFileNameList = new ArrayList<>();
			existingFileNameList.add(fileName);
			postProcessMap.put(key, existingFileNameList);
		}
	}

	public String getBannerPage(String key)
			throws URISyntaxException, StorageException, FileNotFoundException, IOException {
		CloudBlobContainer container = containerInfo();
		CloudBlobDirectory transitDirectory = getDirectoryName(container, ROOT_DIRECTORY, BANNER_DIRECTORY);
		String bannerFileName = BANNER_PAGE + key + PDF_EXTENSION;
		CloudBlockBlob blob = transitDirectory.getBlockBlobReference(bannerFileName);
		File source = new File(bannerFileName);
		blob.downloadToFile(source.getAbsolutePath());
		return bannerFileName;
	}

	public CloudBlobContainer containerInfo() {
		CloudBlobContainer container = null;
		try {
			CloudStorageAccount account = CloudStorageAccount.parse(connectionNameKey);
			CloudBlobClient serviceClient = account.createCloudBlobClient();
			container = serviceClient.getContainerReference(containerName);
		} catch (Exception exception) {
			logger.info("Exception:" + exception.getMessage());
		}
		return container;
	}

	public String currentDateTime() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.format(date);
	}

	public CloudBlobDirectory getDirectoryName(CloudBlobContainer container, String directoryName,
			String subDirectoryName) throws URISyntaxException {
		CloudBlobDirectory cloudBlobDirectory = container.getDirectoryReference(directoryName);
		if (StringUtils.isBlank(subDirectoryName)) {
			return cloudBlobDirectory;
		}
		return cloudBlobDirectory.getDirectoryReference(subDirectoryName);
	}

	private String getFileNameFromBlobURI(URI uri) {
		String[] fileNameList = uri.toString().split(PostProcessingConstant.FILE_SEPARATION);
		Optional<String> fileName = Optional.empty();
		if (fileNameList.length > 1)
			fileName = Optional.ofNullable(fileNameList[fileNameList.length - 1]);
		return fileName.get();
	}

	public void deletePreviousLogFile() {
		LocalDate date = LocalDate.now();
		LocalDate previousDate = date.minusDays(1);
		File previousDayLogFile = new File(LOG_FILE + previousDate + ".log");
		if (previousDayLogFile.exists()) {
			previousDayLogFile.delete();
		}
	}

	public String zipFileTransferToArchive(String currentDate, String rootDirectoryName, String targetDirectory)
			throws IOException {
		String message = "post processing archive completed successfully";
		try {
			CloudBlobContainer container = containerInfo();
			BlobContainerClient blobContainerClient = getBlobContainerClient(connectionNameKey, containerName);
			CloudBlobDirectory transitDirectory = getDirectoryName(container, rootDirectoryName, "");
			Iterable<BlobItem> listBlobs = blobContainerClient.listBlobsByHierarchy(rootDirectoryName);
			List<String> files = new LinkedList<String>();
			String archiveZipFileName = currentDate + "-" + ARCHIVE_VALUE;
			for (BlobItem blobItem : listBlobs) {
				String fileNames[] = StringUtils.split(blobItem.getName(), "/");
				String fileName = fileNames[fileNames.length - 1];
				File file = new File(fileName);
				CloudBlockBlob blob = transitDirectory.getBlockBlobReference(fileName);
				blob.downloadToFile(file.getPath());
				files.add(fileName);
				blob.delete();
			}
			if (files.size() > 0) {
				ZipUtility zipUtility = new ZipUtility();
				zipUtility.zipProcessing(files, archiveZipFileName);
				copyFileToTargetDirectory(archiveZipFileName, "",targetDirectory);
				deleteFiles(files);
				new File(archiveZipFileName).delete();
			}else {
				message = "no file for archive";
			}
		} catch (Exception exception) {
			logger.info("exception:" + exception.getMessage());
		}
		return message;
	}
}
