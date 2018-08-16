package netsuite;

import com.netsuite.webservices.documents.filecabinet_2017_1.*;
import com.netsuite.webservices.documents.filecabinet_2017_1.File;
import com.netsuite.webservices.documents.filecabinet_2017_1.types.FileAttachFrom;
import com.netsuite.webservices.documents.filecabinet_2017_1.types.MediaType;
import com.netsuite.webservices.platform.common_2017_1.FileSearchBasic;
import com.netsuite.webservices.platform.common_2017_1.FolderSearchBasic;
import com.netsuite.webservices.platform.core_2017_1.*;

import com.netsuite.webservices.platform.core_2017_1.types.RecordType;
import com.netsuite.webservices.platform.core_2017_1.types.SearchMultiSelectFieldOperator;
import com.netsuite.webservices.platform.core_2017_1.types.SearchStringFieldOperator;
import com.netsuite.webservices.platform.messages_2017_1.ApplicationInfo;
import com.netsuite.webservices.platform.messages_2017_1.ReadResponse;
import com.netsuite.webservices.platform.messages_2017_1.WriteResponse;
import com.netsuite.webservices.platform_2017_1.NetSuiteBindingStub;
import com.netsuite.webservices.platform_2017_1.NetSuitePortType;
import com.netsuite.webservices.platform_2017_1.NetSuiteServiceLocator;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import javax.xml.rpc.ServiceException;
import java.io.*;
import java.rmi.RemoteException;

public class NSClient	{

    final private String NS_WSDL_VERSION = "2017_1";

    /**
     * Proxy class that abstracts the communication with the netsuite Web
     * Services. All netsuite operations are invoked as methods of this class.
     */
    private NetSuitePortType _port;

    private NSAccount nsAccount;

    private String nsEnvironment;

    public NSClient(NSAccount account, String environment) throws ServiceException, IOException   {

        this.nsAccount = account;
        this.nsEnvironment = environment;

        String nsWebServiceURL = "";

        if (this.nsEnvironment.equals("Production")) {
            nsWebServiceURL = this.nsAccount.getProductionWebServicesDomain();
        } else if (this.nsEnvironment.equals("Sandbox")) {
            nsWebServiceURL = this.nsAccount.getSandboxWebServicesDomain();
        }

        // In order to use SSL forwarding for SOAP messages. Refer to FAQ for details
        System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");

        NetSuiteServiceLocator service = new NetSuiteServiceLocator();

        service.setNetSuitePortEndpointAddress(nsWebServiceURL);

        // Saving of cookies needs to be disabled, otherwise after login request which sets JSESSIONID,
        // consecutive requests which includes authentication (passport) header together with JSESSIONID cookie end with "Unexpected error".
        service.setMaintainSession(false);

        // Get the service port (to the correct datacenter)
        _port = service.getNetSuitePort();

        SOAPHeaderElement applicationIdHeader = createApplicationIdHeaders();
        ((Stub) _port).setHeader(applicationIdHeader);
        SOAPHeaderElement passportHeader = createPassportHeader();
        ((Stub) _port).setHeader(passportHeader);

        // Setting client timeout to 2 hours for long running operations
        ((NetSuiteBindingStub) _port).setTimeout(1000 * 60 * 60 * 2);
    }

    public NSAccount getNSAccount() {
        return this.nsAccount;
    }

    private SOAPHeaderElement createApplicationIdHeaders() {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.setApplicationId("79927DCC-D1D8-4884-A7C5-F2B155FA00F3"); // TODO: Need own app ID?
        return new SOAPHeaderElement("urn:messages_" + NS_WSDL_VERSION + ".platform.webservices.netsuite.com", "applicationInfo", applicationInfo);
    }

    private SOAPHeaderElement createPassportHeader() {
        Passport passport = createPassport();
        return new SOAPHeaderElement("urn:core_" + NS_WSDL_VERSION + ".platform.webservices.netsuite.com", "passport", passport);
    }

    private Passport createPassport() {
        RecordRef role = new RecordRef();
        role.setInternalId(this.nsAccount.getRoleId());

        Passport passport = new Passport();
        passport.setEmail(this.nsAccount.getAccountEmail());
        passport.setPassword(this.nsAccount.getAccountPassword());
        passport.setAccount(this.nsAccount.getAccountId());
        passport.setRole(role);
        return passport;
    }

    public void tryToLogin() throws RemoteException {
        Passport passport = createPassport();
        Status status = (_port.login(passport)).getStatus();

        if (!status.isIsSuccess()) {
            throw new IllegalStateException(new Throwable("Netsuite SuiteTalk login request call was unsuccessful."));
        }
    }

    public File downloadFile(String fileInternalId) throws RemoteException {
        RecordRef recordRef = new RecordRef();
        recordRef.setInternalId(fileInternalId);
        recordRef.setType(RecordType.file);

        ReadResponse readResponse = null;

        try {
            readResponse = _port.get(recordRef);
        } catch (Exception ex) {
            return null;
        }

        return (File)readResponse.getRecord();
    }

    public WriteResponse uploadFile(String nsFileName, String filePath, String fileInternalId, String nsParentFolder, String fileType) throws RemoteException {
        Boolean shouldUpdate = false;

        File uploadFile = null;

        if (fileInternalId != null) {
            uploadFile = downloadFile(fileInternalId);
            shouldUpdate = true;
        } else {
            uploadFile = new File();
            uploadFile.setName(nsFileName);
            RecordRef folderRef = new RecordRef();
            folderRef.setInternalId(nsParentFolder);
            uploadFile.setFolder(folderRef);
        }

        uploadFile.setAttachFrom(FileAttachFrom._computer);

        if (fileType != null) {
            if (fileType.trim().toLowerCase().equals("plaintext"))
                uploadFile.setFileType(MediaType._PLAINTEXT);
            else if (fileType.trim().toLowerCase().equals("image"))
                uploadFile.setFileType(MediaType._IMAGE);
            else if (fileType.trim().toLowerCase().equals("csv"))
                uploadFile.setFileType(MediaType._CSV);
            else
                uploadFile.setFileType(MediaType._PLAINTEXT);
        }
        else {
            uploadFile.setFileType(MediaType._PLAINTEXT);
        }

        uploadFile.setContent(loadFile(filePath));
        uploadFile.setFileSize(null);

        if (shouldUpdate) {
            return _port.update(uploadFile);
        }

        return _port.add(uploadFile);
    }

    private byte[] loadFile(String sFileName) {
        InputStream inFile = null;
        byte[] data = null;

        try {
            java.io.File file = new java.io.File(sFileName);
            inFile = new FileInputStream(file);
            data = new byte[(int) file.length()];
            inFile.read(data, 0, (int) file.length());
            inFile.close();
        }
        catch (Exception ex) {
            return null;
        }

        return data;
    }

    public SearchResult getSubFolders(String parentFolderId) throws RemoteException {
        if (parentFolderId != null && !parentFolderId.isEmpty()) {
            RecordRef parentFolderRef = new RecordRef();
            parentFolderRef.setInternalId(parentFolderId);

            SearchMultiSelectField parentFolderMultiSelectFieldSearch  = new SearchMultiSelectField();
            RecordRef[] recordRefs = new RecordRef[1];
            recordRefs[0] = parentFolderRef;

            parentFolderMultiSelectFieldSearch.setSearchValue(recordRefs);
            parentFolderMultiSelectFieldSearch.setOperator(SearchMultiSelectFieldOperator.anyOf);

            FolderSearchBasic subDirectoryFolderSearch = new FolderSearchBasic();
            subDirectoryFolderSearch.setParent(parentFolderMultiSelectFieldSearch);

            // Invoke search() web services operation
            SearchResult results = _port.search(subDirectoryFolderSearch);

            // Process result
            if (results.getStatus().isIsSuccess()) {
                return results;
            }
        }

        return null;
    }

    public String searchFolder(String folder, String parentFolderId) throws RemoteException {
        RecordRef parentFolderRef = new RecordRef();
        parentFolderRef.setInternalId(parentFolderId);

        RecordRef[] rr = new RecordRef[1];
        rr[0] = parentFolderRef;

        SearchMultiSelectField smsf = new SearchMultiSelectField();
        smsf.setSearchValue(rr);
        smsf.setOperator(SearchMultiSelectFieldOperator.anyOf);

        FolderSearchBasic subDirectoryFolderSearch = new FolderSearchBasic();
        SearchStringField nameField = new SearchStringField();
        nameField.setSearchValue(folder);
        nameField.setOperator(SearchStringFieldOperator.is);
        subDirectoryFolderSearch.setName(nameField);
        subDirectoryFolderSearch.setParent(smsf);

        SearchResult results = _port.search(subDirectoryFolderSearch);

        if (results.getStatus().isIsSuccess()) {

            if (results.getTotalRecords() > 0) {
                RecordList myRecordlist = results.getRecordList();

                if (myRecordlist != null) {
                    for(int i = 0; i < results.getTotalRecords(); i++) {
                        Folder foundFolder = (Folder) myRecordlist.getRecord(i);

                        if (foundFolder.getName().equals(folder)) {
                            return foundFolder.getInternalId();
                        }
                    }
                }
            }
        }

        return null;
    }

    public String createFolder(String folder, String parentFolderId) throws RemoteException {
        RecordRef parentFolderRef = new RecordRef();
        parentFolderRef.setInternalId(parentFolderId);

        Folder myFolder = new Folder();
        myFolder.setParent(parentFolderRef);
        myFolder.setName(folder);

        WriteResponse response = null;

        try {
            response = _port.add(myFolder);
        } catch (Exception ex) {
            return null;
        }

        if (response != null && response.getStatus().isIsSuccess()) {
            return ((RecordRef)response.getBaseRef()).getInternalId();
        }

        return null;
    }

    public String searchFile(String fileName, String parentFolderId, String projectSettingsRootFolderId) throws RemoteException {
        RecordRef parentFolderRef = new RecordRef();
        parentFolderRef.setInternalId(parentFolderId);

        RecordRef[] rr = new RecordRef[1];
        rr[0] = parentFolderRef;

        SearchMultiSelectField smsf = new SearchMultiSelectField();
        smsf.setSearchValue(rr);
        smsf.setOperator(SearchMultiSelectFieldOperator.anyOf);

        SearchStringField nameField = new SearchStringField();
        nameField.setOperator(SearchStringFieldOperator.is);
        nameField.setSearchValue(fileName);

        FileSearchBasic fileSearchBasic = new FileSearchBasic();
        fileSearchBasic.setFolder(smsf);
        fileSearchBasic.setName(nameField);

        SearchResult results = null;

        try {
            results = _port.search(fileSearchBasic);
        } catch (Exception ex) {
            return null;
        }

        if (results != null && results.getStatus().isIsSuccess()) {
            RecordList myRecordlist = results.getRecordList();

            if (myRecordlist != null && myRecordlist.getRecord() != null) {
                File foundFile = null;

                if (parentFolderId.equals(projectSettingsRootFolderId)) {
                    foundFile = (File) myRecordlist.getRecord(results.getTotalRecords()-1);
                } else {
                    foundFile = (File) myRecordlist.getRecord(0);
                }

                if (foundFile != null) {
                    return foundFile.getInternalId();
                }
            }
        }

        return null;
    }
}
