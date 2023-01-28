//Script to upload attachments from a directory and its subdirectories to a selected confluence page identified by its ID.


/******************************************************************************/
/***********************  SCRIPT CONFIGURATION  *******************************/
/******************************************************************************/

CONFLUENCE_BASE_URL = "https://ws001.sspa.juntadeandalucia.es/confluence"
PAGE_ID = 136020393
DIRECTORY_PATH = "C:\\SIS_TEC_SERVICIOS\\SIS_JA40_SVC_Coanh" //BEWARE SPECIAL CHARACTERS LIKE ACCENTS
SESSION_COOKIE = "D313727F84AEAA8EDBB82679AD6349AA"
RECURSE = true // set true to get all files in subdirectories, false to upload only the files in the directory specified before
AUTO_RENAME = true //set true to rename automatically the files with a repeated name.

/******************************************************************************/
/******************************************************************************/
/******************************************************************************/

def directory = new File(DIRECTORY_PATH)
def fileList = []
def duplicatedFileList = []

if(RECURSE){
    directory.eachFileRecurse{
        if(it.isFile()){
            addFileToList(fileList, duplicatedFileList, it)
        }
    }
} else {
    directory.eachFile{
        if(it.isFile()){
            addFileToList(fileList, duplicatedFileList, it)
        }
    }
}


if(duplicatedFileList.size() > 0){
    showFileList(duplicatedFileList, 
        "The following files have a duplicated name. Please rename them manually to continue or set AUTO_RENAME to true to rename them automatically.")
}
else{
    showFileList(fileList, "Next " + ColorEnum.MAGENTA.value + fileList.size() + " files" + ColorEnum.DEFAULT.value 
        + " are going to be uploaded to the confluence page: " + PAGE_ID + "\n")

    if(askContinue("Do you want to upload these file to the page with ID " + PAGE_ID + "? [yes]/no: ")){
        createRequest(fileList, false)
    }
}




def addFileToList(fileList, duplicatedFileList, file){
    if(!file.name.startsWith("~\$") && file.name != "Thumbs.db"){
        def repeatedName = isNameRepeated(fileList, file.name)
        def counter = 0

        if(repeatedName && AUTO_RENAME){
            while(repeatedName){ //add (x) in the end of the file name until its not repeated
                counter++
                def newName = addModifierToName(file.path, counter)
                repeatedName = isNameRepeated(fileList, newName)

                if(!repeatedName){
                    println(ColorEnum.RED.value + "File " + file.name + " is repeated" + ColorEnum.DEFAULT.value)
                    println(file)
                    println(" is renamed to " +  newName)
                    file.renameTo(newName)
                    file = new File(newName)
                }
            }

            fileList << file
        }
        else if(repeatedName && !AUTO_RENAME){
            duplicatedFileList << file
        }
        else{
            fileList << file
        }
    }
}

def createRequest(fileList, divideList){
    println("Uploading files to confluence page with ID: " + PAGE_ID)
    println("...")
    def url = CONFLUENCE_BASE_URL + "/rest/api/content/" + PAGE_ID + "/child/attachment"
    def request;

    try{
        String boundary = UUID.randomUUID().toString();

        connection = (HttpURLConnection) new URL(url).openConnection()
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("X-Atlassian-Token", "no-check")
        connection.setRequestProperty("Cookie", "JSESSIONID=" + SESSION_COOKIE) //auth cookie, you can use another way to authenticate
        connection.setDoOutput(true)

        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());

        addFilesToBody(dataOutputStream, fileList, boundary)
        dataOutputStream.flush()
        dataOutputStream.close()

        showResponseMessage(connection)

        if(isSuccesfull(connection.getResponseCode())){
            showUploadedFiles(fileList)
        }
    }catch(OutOfMemoryError e){
        if(fileList.size() <= 1){
            println(ColorEnum.RED.value + 
                "The following file is too big to be uploaded" +
                ColorEnum.DEFAULT.value)
        }
        else{
            println(ColorEnum.RED.value + 
                "File list is too big to be uploaded. List will be divided into smaller lists and they'll be sent again" +
                ColorEnum.DEFAULT.value)

            //if it's the main list it will ask to divide the list in smaller ones to upload them
            if(!divideList){
                divideList = askContinue("Do you want to divide the list and retry? [yes]/no: ")
            }

            if(divideList){
                def listDivided = fileList.collate((fileList.size().intdiv(2)))
                listDivided.each{
                    showFileList(it, "Next " + ColorEnum.MAGENTA.value + it.size() + " files" + ColorEnum.DEFAULT.value 
                        + " sublist is going to be uploaded to page with ID: " + PAGE_ID + "\n")
                    createRequest(it, divideList)
                }
            }     
        }   
    }catch(Exception e){
        println(ColorEnum.RED.value)
        println("Exception uploading file list to page with ID:" + PAGE_ID)
        e.printStackTrace()
        println(ColorEnum.DEFAULT.value)
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

def showResponseMessage(connection){
    def statusCode = connection.getResponseCode()
    def inputStreamReader = null
    def response = new StringBuilder()
    def line = null
    def bufferedReader

    println("'\r\nSTATUS CODE: " + statusCode)

    if(isSuccesfull(statusCode)){ //success
        inputStreamReader = new InputStreamReader(connection.getInputStream(), "utf-8")
    }
    else{ //error
        println(ColorEnum.RED.value)
        inputStreamReader = new InputStreamReader(connection.getErrorStream(), "utf-8")
    }

    bufferedReader = new BufferedReader(inputStreamReader)
    while((line = bufferedReader.readLine()) != null){
        response.append(line)
    }

    println(response + ColorEnum.DEFAULT.value)
}

def isSuccesfull(statusCode){
    return (statusCode >= 200 && statusCode < 300)
}

def isNameRepeated(fileList, name){
    return fileList.stream().anyMatch(fileElement -> fileElement.name.equals(name))
}

//Adds a modifier to the name before the extension
def addModifierToName(name, modifier){ 
    def subname = name.substring(0, name.lastIndexOf("."))
    def extension = name.substring(name.lastIndexOf("."))

    return (subname + "(" + modifier + ")" + extension)
}

def askContinue(text){
    def doContinue = System.console().readLine(text)
    return !(doContinue ==~ /(?i)no/)
}

def showFileList(fileList, headerMessage){
    println("\n\n")
    println(headerMessage)
    fileList.each{
        println(it.path)
    }
    println("\n")
}

def showUploadedFiles(fileList){
    fileList.each{
        println("\n" + it.name + "[" + ColorEnum.GREEN.value + "OK" + ColorEnum.DEFAULT.value + "]")
    }
}


public enum ColorEnum{
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    MAGENTA("\u001B[35m"),
    DEFAULT("\u001B[0m")

    String value

    ColorEnum(String value){
        this.value = value
    }
}



