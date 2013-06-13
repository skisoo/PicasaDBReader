import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class PMPDB {
	private static final String PARAM_OUTPUT_FOLDER = "output";

	private static final String PARAM_PICASA_DB_FOLDER = "folder";

	private static final String APPTITLE = "PMPDB";

	private static final String HELP = "read all the PMP files containing the Picasa Database and the file " +
	                		"containing indexes thumbindex.db. \nThis program will create 3 csv files: albumdata.csv " +
	                		"(album database), catdata.csv (category database), imagedata.csv (picture database).\n\nParameters:\n";
	
	HashMap<String, ArrayList<String>> catdata;
	HashMap<String, ArrayList<String>> albumdata;
	HashMap<String, ArrayList<String>> imagedata;
	String folder;
	Indexes indexes;
	
	public PMPDB(String folder) {
		this.folder = folder;
		indexes = new Indexes(folder);
	}
	
	public void populate() throws Exception{
		indexes.Populate();
		catdata = getTable("catdata");
		albumdata = getTable("albumdata");
		imagedata = getTable("imagedata");
		ArrayList<String> is = new ArrayList<String>();
		for(Long l:indexes.indexes){
			is.add(l.toString());
		}
		ArrayList<String> ois = new ArrayList<String>();
		for(Long l:indexes.originalIndexes){
			ois.add(l.toString());
		}
		imagedata.put("index",is);
		imagedata.put("original index", ois);
		imagedata.put("name", indexes.names);
		
	}
	
	
	private HashMap<String, ArrayList<String>>  getTable(final String table) throws Exception{
        //filter on table_*.pmp
        FilenameFilter filter = new FilenameFilter() { 
            @Override
            public boolean accept(File dir, String filename)
            { 
            	return filename.startsWith(table+"_") && filename.endsWith(".pmp");
            }
        };
        
        File[] files = new File(folder).listFiles(filter);
        HashMap<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();
        
        for(int i=0; i<files.length; i++){
            String filename 	= files[i].getName();
		    String fieldname	= filename.replace(table+"_", "").replace(".pmp", "");
	            
		    //System.out.print(fieldname+" ");
            data.put(fieldname, readColumn(folder+filename)); //saving column fieldname
        }
        
        
        return data;
    }
	
	private static ArrayList<String> readColumn(String file) throws Exception{
		ArrayList<String> l = new ArrayList<String>();
        DataInputStream din = new DataInputStream
            (new BufferedInputStream
             (new FileInputStream(file)));
        long magic = ReadFunctions.readUnsignedInt(din); //file start with a magic sequence
        int type = ReadFunctions.readUnsignedShort(din); // then the entries type
         if ((magic=ReadFunctions.readUnsignedShort(din)) != 0x1332) {  //first constant
            throw new IOException("Failed magic2 "+Long.toString(magic,16));
        }
        if ((magic=ReadFunctions.readUnsignedInt(din)) != 0x2) {  //second constant
            throw new IOException("Failed magic3 "+Long.toString(magic,16));
        }
        if ((magic=ReadFunctions.readUnsignedShort(din)) != type) { // type again
            throw new IOException("Failed repeat type "+
                                  Long.toString(magic,16));
        }
        if ((magic=ReadFunctions.readUnsignedShort(din)) != 0x1332) {  //third constant
            throw new IOException("Failed magic4 "+Long.toString(magic,16));
        }
        long v = ReadFunctions.readUnsignedInt(din); //number of entries
        
        
        // records.
        for(int i=0; i<v; i++){
            if (type == 0) {
                l.add(ReadFunctions.dumpStringField(din));
            }
            else if (type == 0x1) {
                l.add(ReadFunctions.dump4byteField(din));
            }
            else if (type == 0x2) {
                l.add(ReadFunctions.dumpDateField(din));
            }
            else if (type == 0x3) {
                l.add(ReadFunctions.dumpByteField(din));
            }
            else if (type == 0x4) {
                l.add(ReadFunctions.dump8byteField(din));
            }
            else if (type == 0x5) {
                l.add(ReadFunctions.dump2byteField(din));
            }
            else if (type == 0x6) {
                l.add(ReadFunctions.dumpStringField(din));
            }
            else if (type == 0x7) {
                l.add(ReadFunctions.dump4byteField(din));
            }
            else {
                throw new IOException("Unknown type: "+Integer.toString(type,16));
            }
        }
        din.close();
        return l;
    }
	
	public void writeCSVs(String output) throws Exception{
		writeCSV("catdata", catdata, output);
		writeCSV("imagedata", imagedata, output);
		writeCSV("albumdata", albumdata, output);
	}
	
    private static void writeCSV(String table, HashMap<String, ArrayList<String>> data, String output) throws Exception{
    	// not all files have the same number of elements, get the maximum size for a table
        int max = 0;
        for (String key:data.keySet()){
            int size = data.get(key).size();
            //System.out.println(key+":"+size);
            if(size>max)
                max=size;
        }
        
        StringBuilder s = new StringBuilder();
        
        // column names
        for (String key:data.keySet()){
                s.append(key);
                s.append(";");
            }
        s.append("\n");
        
        
        for(int i=0; i<max ; i++){
            for (String key:data.keySet()){
                
                // for column that have less elements that the max, fill the end with %empty%
                if(data.get(key).size()>i){
                    s.append(data.get(key).get(i));
                }else{
                    s.append("%empty%");
                }
                s.append(";");
            }
            s.append("\n");
        }
    FileWriter fw = new FileWriter(output+table+".csv");
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(s.toString());
        bw.close();
    }
	
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options options = new Options();
    	options.addOption("h","help", false, "prints the help content");
    	options.addOption(OptionBuilder.withArgName("srcFolder").hasArg().withDescription("Picasa DB folder. Default is " + EnvironmentVariables.DEFAULT_PICASA_DB_PATH).create(PARAM_PICASA_DB_FOLDER));
    	options.addOption(OptionBuilder.withArgName("outputFolder").hasArg().isRequired().withDescription("output folder").create(PARAM_OUTPUT_FOLDER));
    	
    	CommandLineParser parser = new GnuParser();
    	String folder=null;
    	String output=null;
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
            		throw new Exception("output folder does not exist:"+output);
            	}
            }
        }
        catch( ParseException exp ) {
            // oops, something went wrong
        	
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showHelp(options);
            System.exit(1);
        }
        
        PMPDB db = new PMPDB(folder);
        db.populate();
        db.writeCSVs(output);
	}

	private static void showHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(APPTITLE, 
				HELP, 
				options, "\n", true);
	}
}
