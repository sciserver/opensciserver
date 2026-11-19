package org.sciserver.sciquery.output;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import org.sciserver.racm.utils.model.RACMBaseModel;
import org.springframework.http.MediaType;

public class Output{

	public static enum OutputFormat {JSON, CSV, TSV, VOTABLE, XML, FITS, DATABASETABLE}
	
	public Output(){
		
	}
	
	
	private static final Map<OutputFormat,MediaType> contentTypeHeader = new HashMap<>();
	static {
		contentTypeHeader.put(OutputFormat.JSON, MediaType.APPLICATION_JSON);
		contentTypeHeader.put(OutputFormat.CSV, MediaType.TEXT_PLAIN);
		contentTypeHeader.put(OutputFormat.TSV, MediaType.TEXT_PLAIN);
		contentTypeHeader.put(OutputFormat.VOTABLE, MediaType.APPLICATION_XML);
		contentTypeHeader.put(OutputFormat.XML, MediaType.APPLICATION_XML);
		contentTypeHeader.put(OutputFormat.FITS, MediaType.APPLICATION_OCTET_STREAM);
	}
	
	public static MediaType getContentTypeHeader(OutputFormat outputFormat){
		return contentTypeHeader.get(outputFormat);
	}
	
}