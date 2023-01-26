//Script to upload attachments from a directory and its subdirectories to a selected confluence page identified by its ID.


/******************************************************************************/
/*************************SCRIPT CONFIGURATION*********************************/
/******************************************************************************/

CONFLUENCE_BASE_URL = "<CONFLUENCE_BASE_URL>"
PAGE_ID = 123456789
DIRECTORY_PATH = "<PATH_TO_DIRECTORY>"
SESSION_COOKIE = "AUTH_SESSION_COOKIE"
RECURSE = true // set true to get all files in subdirectories, false to upload only the directorie's files
RETRIES = 0 //number or retries in case the request fails

/******************************************************************************/
/***************************END CONFIGURATION**********************************/
/******************************************************************************/

def directory = new File(DIRECTORY_PATH)
def fileList = []

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

createRequest(fileList)



def createRequest(fileList){
    println("Uploading files to confluence page with ID: " + PAGE_ID)
    def url = CONFLUENCE_BASE_URL + "/rest/api/content/" + PAGE_ID + "/child/attachment"
    def request;

    try{
        String boundary = UUID.randomUUID().toString();

        connection = (HttpURLConnection) new URL(url).openConnection()
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("X-Atlassian-Token", "no-check")
        connection.setRequestProperty("Cookie", "JSESSIONID=" + SESSION_COOKIE) //auth cookie, you can use another auth method
        connection.setDoOutput(true)

        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());

        addFilesToBody(dataOutputStream, fileList, boundary)
        dataOutputStream.flush()
        dataOutputStream.close()


        def inputStreamReader = new InputStreamReader(connection.getInputStream(), "utf-8")
        def response = new StringBuilder()
        def line = null;

        def bufferedReader = new BufferedReader(inputStreamReader)
        while((line = bufferedReader.readLine()) != null){
            response.append(line)
        }

        println(response)

    }catch(Exception e){
        println("Exception uploading file list")
        println(e)
    }
}

def addFilesToBody(dataOutputStream, fileList, boundary){
    fileList.each{
        dataOutputStream.writeBytes("--" + boundary + "\r\n")
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + it.name + "\"\r\n\r\n")
        dataOutputStream.write(it.bytes)
        dataOutputStream.writeBytes("\r\n")
    }

        dataOutputStream.writeBytes("--" + boundary + "--\r\n");
}

def isSuccesfull(statusCode){
    return (statusCode >= 200 && statusCode < 300)
}




