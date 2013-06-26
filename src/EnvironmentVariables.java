import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;

public class EnvironmentVariables {
	public static final String DEFAULT_PICASA_DB_PATH = EnvironmentVariables.expandEnvVars("%LOCALAPPDATA%/Google/Picasa2/db3");

	public static String expandEnvVars(String text) {        
	    Map<String, String> envMap = System.getenv();       
	    for (Entry<String, String> entry : envMap.entrySet()) {
	        String key = entry.getKey();
	        String value = entry.getValue().replace('\\', '/');
	        text = text.replaceAll("\\%" + key + "\\%", value);
	    }
	    return text;
	}

	public static String getPicasaDBFolder(CommandLine line, String parameterNameForPicasaDBFolder) throws Exception {
		String folder;
		// under M$-Windows7 picasa db is usually at %LOCALAPPDATA%/Google/Picasa2/db3
		// that will be expaned to C:/Users/{UserName}/AppData/Local/Google/Picasa2/db3
		if(line.hasOption(parameterNameForPicasaDBFolder)){
			folder = EnvironmentVariables.expandEnvVars(line.getOptionValue(parameterNameForPicasaDBFolder));
		} else {
			folder = EnvironmentVariables.DEFAULT_PICASA_DB_PATH;
		}
		if(!folder.endsWith(File.separator)){
			folder += File.separator;
		}
		if(! new File(folder).exists()){
			throw new Exception("Source folder does not exist:"+folder);
		}
		return folder;
	}
}
