//Script to upload attachments from a directory and its subdirectories to a selected confluence page identified by its ID.


/******************************************************************************/
/*************************SCRIPT CONFIGURATION*********************************/
/******************************************************************************/

CONFLUENCE_BASE_URL = "https://ws001.sspa.juntadeandalucia.es/confluence"
PAGE_ID = 134152138
DIRECTORY_PATH = "C:\\SIS_TEC_SERVICIOS\\SIS_JA00_SVC_Athos"
SESSION_COOKIE = ""
RECURSE = true // set true to get all files in subdirectories, false to upload only the directorie's files

/******************************************************************************/
/***************************END CONFIGURATION**********************************/
/******************************************************************************/

def directory = new File(DIRECTORY_PATH)
def fileList = []

//Get file from directory
if(RECURSE){
    directory.eachFileRecurse{
        if(it.isFile()){
            fileList << it
        }
    }
} else {
    directory.eachFile{
        if(it.isFile()){
            fileList << it
        }
    }
}

fileList.each{
    uploadAttachment(it)
}


def uploadAttachment(file){
    println("Uploading " + file.name)
    createRequest(file)
}

def createRequest(file){
    def url = CONFLUENCE_BASE_URL + "/rest/api/content/" + PAGE_ID + "/child/attachment"
    println(url)
    def request;

    try{
        def fileBytes = file.bytes //transform file to byte[]
        String boundary = UUID.randomUUID().toString();

        //create http post connection
        request = (HttpURLConnection) new URL(url).openConnection()
        request.setRequestMethod("POST")
        request.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        request.setRequestProperty("Accept", "application/json")
        request.setRequestProperty("X-Atlassian-Token", "no-check")
        request.setDoOutput(true)

        DataOutputStream dos = new DataOutputStream(request.getOutputStream());

        def fileInputStream = new FileInputStream(file);

        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.name + "\"\r\n");
        dos.write(fileBytes);
        dos.writeBytes("\r\n");
        dos.writeBytes("--" + boundary + "--\r\n");
        dos.flush();

        def statusCode = request.getResponseCode()
        println(statusCode)

        //add file to post body
        // OutputStream os = request.getOutputStream()
        // os.write(fileBytes, 0, fileBytes.length)
        

    }catch(Exception e){
        println("Exception uploading file " + file.name)
        println(e)
    }
}




