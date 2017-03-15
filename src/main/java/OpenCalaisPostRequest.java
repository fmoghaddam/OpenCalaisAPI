import java.io.File;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final public class OpenCalaisPostRequest {

	private static final Logger LOG = Logger.getLogger(OpenCalaisPostRequest.class.getCanonicalName());
	
	private static final String CALAIS_URL = "https://api.thomsonreuters.com/permid/calais";
	public static String uniqueAccessKey = "k2njsQnXBfkm8rqe6kni0Rgepi19yy4k";
	private static File input;
	private static File output;
	private static String fileContent;
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			input = new File("input");
			output = new File("output");
			final HttpClient client = HttpClientBuilder.create().build();
			final HttpPost post = new HttpPost(CALAIS_URL);
			final HttpHost proxy = new HttpHost("proxy.fiz-karlsruhe.de", 8888, "http");
			final RequestConfig config = RequestConfig.custom().setProxy(proxy).build();

			post.setConfig(config);
			post.setHeader("X-AG-Access-Token", uniqueAccessKey);
			post.setHeader("outputformat", "application/json");
			post.setHeader("Content-Type", "text/raw");

			for (final File file : input.listFiles()) {
				if (file.isFile() && FilenameUtils.getExtension(file.getName()).equals("txt")) {
					Thread.sleep(100);
					final FileEntity entity = new FileEntity(file);
					fileContent = IOUtils.toString(entity.getContent(), "UTF-8");
					post.setEntity(entity);
					
					final HttpResponse response = client.execute(post);
					if(response.getStatusLine().getStatusCode()!=200){
						LOG.error("Could not finish request for file "+file.getName()+". Reason: "+response.getStatusLine());
						continue;
					}					
					final String jsonResult = EntityUtils.toString(response.getEntity());
					//System.err.println(jsonResult);
					final JSONObject obj = new JSONObject(jsonResult);
					Iterator<?> keys = obj.keys();

					String resultString = fileContent;
					while( keys.hasNext() ) {
					    final String key = (String)keys.next();
					    Object object = obj.get(key);
						if (object instanceof JSONObject ) {
							try{
								final JSONObject jsonObject = (JSONObject)object;
								if(jsonObject.get("_typeGroup").equals("entities")){
									final JSONArray instanceJsonArray = (JSONArray)jsonObject.get("instances");
									final String nameInText = ((JSONObject)instanceJsonArray.get(0)).getString("exact");
									resultString = resultString.replaceAll(nameInText, getTag(jsonObject.get("_type").toString(),false)+nameInText+getTag(jsonObject.get("_type").toString(),true));
								}
							}catch(final JSONException exception){
								LOG.debug(exception.getMessage());
							}
					    }
					}
					FileUtils.writeStringToFile(new File(output,"annotated_"+file.getName()),resultString,Charset.defaultCharset());
					LOG.info("File "+file.getName()+" has been processed and annotated.");
				} else {
					LOG.warn("Skipping " + file.getAbsolutePath()+". Either it is not a file or extension is not supported. Only TXT is supported for now. ");
				}
			}

		} finally {
			httpclient.close();
		}

	}
	
	private static String getTag(final String tag,final boolean isEndingTag) {
		if(isEndingTag){
			return "</"+tag.toUpperCase()+">";
		}else{
			return "<"+tag.toUpperCase()+">";
		}
		
	}

}