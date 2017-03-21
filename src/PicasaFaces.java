import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;


public class PicasaFaces {
	private static final String PARAM_OUTPUT_FOLDER = "output";
	private static final String PARAM_PICASA_DB_FOLDER = "folder";
	PMPDB db;
	HashMap<String, String> personsId;
	HashMap<String, ArrayList<Face>> personFaces;
	HashMap<Long, Image> images;

	public PicasaFaces(String folder) {
		db = new PMPDB(folder);
		personsId = new HashMap<String, String>();
		personFaces = new HashMap<String, ArrayList<Face>>();
		images = new HashMap<Long, Image>();
	}
	
	public void populate() throws Exception{
		db.populate();
	}
	
	public void populatePersons(){
		ArrayList<String> tokens = db.albumdata.get("token");
		ArrayList<String> name = db.albumdata.get("name");
		int nb = tokens.size();
		personsId.put("0", "nobody");
		
		for (int i=0; i<nb; i++){
			String t = tokens.get(i);
			if(t.startsWith("]facealbum:")){
				personsId.put(t.split(":")[1], name.get(i));
			}
		}
	}
	
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options options = new Options();
    	options.addOption("h","help", false, "print the help content");
    	options.addOption(OptionBuilder.withArgName("srcFolder").hasArg().withDescription("Picasa DB folder. Default is " + EnvironmentVariables.DEFAULT_PICASA_DB_PATH).create(PARAM_PICASA_DB_FOLDER));
    	options.addOption(OptionBuilder.withArgName("outputFolder").hasArg().isRequired().withDescription("output folder").create(PARAM_OUTPUT_FOLDER));
    	options.addOption(OptionBuilder.withArgName("replaceRegex").hasArg().withDescription("regex to change original image path if needed").create("replaceRegex"));
    	options.addOption(OptionBuilder.withArgName("replacement").hasArg().withDescription("replacement for the regex").create("replacement"));
    	options.addOption(new Option("prefix", "add prefix to generated face images"));
    	options.addOption(OptionBuilder.withArgName("convert").hasArg().withDescription("path of convert from imagemagick (including convert itself)").create("convert"));
    	
    	CommandLineParser parser = new GnuParser();
    	String folder=null;
    	String output=null;
    	String regex=null;
    	String replacement=null;
    	boolean prefix =false;
    	String convert = null;
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            if(line.hasOption("h")){
            	showHelp(options);
                System.exit(1);
            }
            
            folder = EnvironmentVariables.getPicasaDBFolder(line, PARAM_PICASA_DB_FOLDER);

        	if(line.hasOption(PARAM_OUTPUT_FOLDER)){
            	output = EnvironmentVariables.expandEnvVars(line.getOptionValue(PARAM_OUTPUT_FOLDER));
                if(!output.endsWith(File.separator)){
                	output += File.separator;
                }
            	if(! new File(output).exists()){
            		new File(output).mkdir();
            	}
            }
            if(line.hasOption("replaceRegex") && !line.hasOption("replacement")){
            	throw new Exception("both 'replaceRegex' and 'replacement' must be present");
            }
            if(!line.hasOption("replaceRegex") && line.hasOption("replacement")){
            	throw new Exception("both 'replaceRegex' and 'replacement' must be present");
            }
            if(line.hasOption("replaceRegex") && line.hasOption("replacement")){
            	regex = line.getOptionValue("replaceRegex");
            	replacement = line.getOptionValue("replacement");
            }
            if(line.hasOption("prefix")){
            	prefix=true;
            }
            if(line.hasOption("convert")){
            	convert=EnvironmentVariables.expandEnvVars(line.getOptionValue("convert"));
            }
        }
        catch( ParseException exp ) {
            // oops, something went wrong
        	
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showHelp(options);
            System.exit(1);
        }
        
        PicasaFaces faces = new PicasaFaces(folder);
        faces.populate();
        faces.populatePersons();
        faces.gatherImages();
        faces.processImages(regex, replacement, output, prefix, convert);

	}

	private static void showHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("PicasaFaces will extract the face information from the Picasa Database and save it in a csv file. " +
				"If the command line contains the argument '-convert' followed by the path to convert, " +
				"then imagemagick will create all the face thumbshots (in the output folder with a folder for each person). " +
				"A string replacement of the image paths can be done if the pictures location is different from the database.", 
				"PicasaFaces" , options , "", true);
	}
	
	public void gatherImages(){
		long nb=db.indexes.entries;
		
		
		for(int i=0; i<nb; i++){
		try {	
			if(db.indexes.indexes.get(i).compareTo(db.indexes.folderIndex)!=0){ // not a folder
				String path = db.indexes.names.get(new Long(db.indexes.indexes.get(i)).intValue()) + db.indexes.names.get(i);
				int w = Integer.parseInt(db.imagedata.get("width").get(i));
				int h = Integer.parseInt(db.imagedata.get("height").get(i));
				Image img = new Image(path, i, w, h);
				String personName = personsId.get(db.imagedata.get("personalbumid").get(i));
	            if(!db.imagedata.get("facerect").get(i).equals("0000000000000001")){
	            	img.hasFaceData=true;
	            	
	            	Face f = img.addFace(db.imagedata.get("facerect").get(i), personName );
	            	if(!db.imagedata.get("personalbumid").get(i).equals("0")){
	            		if(!personFaces.containsKey(personName)){
	            			personFaces.put(personName, new ArrayList<Face>());
	            		}
	            		
	            		personFaces.get(personName).add(f);
	            	}
	            }
				images.put((long)i, img);
			}else{ // folder
            	if(db.indexes.names.get(i).equals("") && db.indexes.originalIndexes.get(i).compareTo(db.indexes.folderIndex)!=0){ // reference
            		if(i>=db.imagedata.get("personalbumid").size()){
            			break;
            		}
            		String personName = personsId.get(db.imagedata.get("personalbumid").get(i));
            		Long originalIndex = db.indexes.originalIndexes.get(i);
            		if(!db.imagedata.get("facerect").get(i).equals("0000000000000001")){
            			images.get(originalIndex).hasChild=true;
    	            	Face f = images.get(originalIndex).addFace(db.imagedata.get("facerect").get(i), personName);
    	            	if(!db.imagedata.get("personalbumid").get(i).equals("0")){
    	            		if(!personFaces.containsKey(personName)){
    	            			personFaces.put(personName, new ArrayList<Face>());
    	            		}
    	            		
    	            		personFaces.get(personName).add(f);
    	            	}
    	            }
            	}// else folder
            }
		}
		catch (Exception ex) 
		{}
		}
	}
	
	public void processImages(String regex, String replacement, String output, boolean prefix, String convert) throws IOException, InterruptedException{
		StringBuilder csv = new StringBuilder("person;prefix;filename;original image path;transformed image path;image width;image height;face x;face y;face width;face height,region_x,region_y,region_w,region_h\n");
		for(String person:personFaces.keySet()){
			File folderPerson = new File(output+person);
			if(convert!=null && !folderPerson.exists()){
				folderPerson.mkdir();
			}
			
			int i=0;
			for(Face f:personFaces.get(person)){
				String path;
				path=FilenameUtils.separatorsToSystem(f.img.path);
				if(regex!=null && replacement!=null){
					path = path.replaceAll(regex, replacement);
				}
				int x=f.x;
				int y=f.y;
				String separator = File.separator;
				if(separator.equals("\\")){
					separator="\\\\";
				}
				String [] file = path.split(separator);
				String prefixStr = "";
				if(prefix){
					prefixStr = ""+ i +"_";
				}
				String filename = output + person + File.separator + prefixStr+file[file.length-1];
				if(convert!=null && new File(filename).exists()){
					System.out.println("Warning, the filename already exist: "+person + File.separator + prefixStr+file[file.length-1]);
				}
				csv.append(person);
				csv.append(";");
				if(prefix){
					csv.append(i);
				}else{
					csv.append("none");
				}
				csv.append(";");
				csv.append(file[file.length-1]);
				csv.append(";");
				csv.append(f.img.path);
				csv.append(";");
				csv.append(path);
				csv.append(";");
				csv.append(f.img.w);
				csv.append(";");
				csv.append(f.img.h);
				csv.append(";");
				csv.append(f.x);
				csv.append(";");
				csv.append(f.y);
				csv.append(";");
				csv.append(f.w);
				csv.append(";");
				csv.append(f.h);
				csv.append(";");
				csv.append(f.region_x);
				csv.append(";");
				csv.append(f.region_y);
				csv.append(";");
				csv.append(f.region_w);
				csv.append(";");
				csv.append(f.region_h);
				csv.append("\n");
				
				if(convert!=null){
					
					if(File.separator.equals("\\")){
						path = "\""+path+"\"";
						filename = "\""+filename+"\"";
					}
					String []cmd = {convert,path, "-crop", f.w+"x"+f.h+"+"+x+"+"+y, filename};
					Process p = Runtime.getRuntime().exec(cmd);
					p.waitFor();
				}
				i++;
			}
		}
		FileWriter fw = new FileWriter(output+"faces.csv");
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(csv.toString());
        bw.close();
	}
}
