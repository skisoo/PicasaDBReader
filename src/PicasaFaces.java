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
	PMPDB db;
	HashMap<String, String> personsId;
	HashMap<String, ArrayList<Face>> personFaces;
	HashMap<Long, Image> images;

	public PicasaFaces(String folder) {
		db = new PMPDB(folder);
		personsId = new HashMap<>();
		personFaces = new HashMap<>();
		images = new HashMap<>();
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
    	options.addOption(OptionBuilder.withArgName("srcFolder").hasArg().isRequired().withDescription("Picasa DB folder").create("folder"));
    	options.addOption(OptionBuilder.withArgName("outputFolder").hasArg().isRequired().withDescription("output folder").create("output"));
    	options.addOption(OptionBuilder.withArgName("replaceRegex").hasArg().withDescription("regex to change original image path if needed").create("replaceRegex"));
    	options.addOption(OptionBuilder.withArgName("replacement").hasArg().withDescription("replacement for the regex").create("replacement"));
    	options.addOption(new Option("prefix", "add prefix to generated face images"));
    	options.addOption(new Option("convert", "use 'convert' from imagemagick to create face images in the %output%/%personName% folder"));
    	
    	CommandLineParser parser = new GnuParser();
    	String folder=null;
    	String output=null;
    	String regex=null;
    	String replacement=null;
    	boolean prefix =false;
    	boolean convert = false;
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            if(line.hasOption("h")){
            	HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "PicasaFaces" , options );
                System.exit(1);
            }
            if(line.hasOption("folder")){
            	folder = line.getOptionValue("folder");
                if(!folder.endsWith(File.separator)){
                	folder += File.separator;
                }
            	if(! new File(folder).exists()){
            		throw new Exception("Source folder does not exist:"+folder);
            	}
            }
            if(line.hasOption("output")){
            	output = line.getOptionValue("output");
                if(!output.endsWith(File.separator)){
                	output += File.separator;
                }
            	if(! new File(output).exists()){
            		throw new Exception("output folder does not exist:"+output);
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
            	convert=true;
            }
        }
        catch( ParseException exp ) {
            // oops, something went wrong
        	
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "PicasaFaces" , options );
            System.exit(1);
        }
        
        PicasaFaces faces = new PicasaFaces(folder);
        faces.populate();
        faces.populatePersons();
        faces.gatherImages();
        faces.processImages(regex, replacement, output, prefix, convert);

	}
	
	public void gatherImages(){
		long nb=db.indexes.entries;
		
		
		for(int i=0; i<nb; i++){
			
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
	}
	
	public void processImages(String regex, String replacement, String output, boolean prefix, boolean convert) throws IOException, InterruptedException{
		StringBuilder csv = new StringBuilder("person;prefix;filename;original image path;transformed image path;image width;image height;face x;face y;face width;face height\n");
		for(String person:personFaces.keySet()){
			File folderPerson = new File(output+person);
			if(!folderPerson.exists()){
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
				String [] file = path.split(File.separator);
				String prefixStr = "";
				if(prefix){
					prefixStr = ""+ i +"_";
				}
				String filename = output + person + File.separator + prefixStr+file[file.length-1];
				if(new File(filename).exists()){
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
				csv.append("\n");
				
				if(convert){
					String [] cmd = {"convert",path, "-crop", f.w+"x"+f.h+"+"+x+"+"+y, filename};
					
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
