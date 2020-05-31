package services;


import chaincodes.BirthCertificate;
import chaincodes.ID;
import com.owlike.genson.Genson;
import components.ConfigurationComponent;
import org.hyperledger.fabric.gateway.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

@Service
public class Services {
    Genson genson = new Genson();
    public String getID(String contractName,String ID) throws IOException {

        ConfigurationComponent configurationComponent = new ConfigurationComponent();
        Gateway gatewayConfig = configurationComponent.setupGatewayConfigurations();

        try (Gateway gateway = gatewayConfig) {

            Contract contract = configurationComponent.getContract(gateway,contractName);

            byte[] result;
            result = contract.evaluateTransaction("getID",ID);

            ID id = genson.deserialize(result,ID.class);

            String pic = getPicture(ID);
            id.setPersonalPicture(pic);

            return genson.serialize(id);

        } catch (ContractException e) {
            e.printStackTrace();
        }

        return ID + "not found";
    }

    public String getPicture(String idNumber) throws IOException {
        String filePath = "../Pictures";
        File file = new File(filePath);
        String image = null;

        if(file!=null){
            for(final File f: file.listFiles()){
                if(!f.isDirectory() && f.getName().equals(idNumber+".png")){
                    String encodeBase64 = null;
                    try{
                        FileInputStream fileInputStream = new FileInputStream(f);
                        byte[] bytes = new byte[(int)f.length()];
                        fileInputStream.read(bytes);
                        encodeBase64 = Base64.getEncoder().encodeToString(bytes);
                        image = "data:image/png;base64,"+ encodeBase64;
                        fileInputStream.close();

                        return image;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return "not found";
    }


    public String getBirthCertificate(String contractName, String ID) throws IOException {

        ConfigurationComponent configurationComponent = new ConfigurationComponent();
        Gateway gatewayConfig = configurationComponent.setupGatewayConfigurations();

        try (Gateway gateway = gatewayConfig) {

            Contract contract = configurationComponent.getContract(gateway,contractName);

            byte[] result;
            result = contract.evaluateTransaction("getBirthCertificate",ID);
            return new String(result);

        } catch (ContractException e) {
            e.printStackTrace();
        }

        return ID + "not found";
    }

    public boolean issueID(ID id) throws IOException {

        ConfigurationComponent configurationComponent = new ConfigurationComponent();
        Gateway gatewayConfig = configurationComponent.setupGatewayConfigurations();

        try (Gateway gateway = gatewayConfig) {

            Contract contract = configurationComponent.getContract(gateway, "IDContractAtCivil");
            Contract contract2 = configurationComponent.getContract(gateway, "IDContractFromHome");

            byte[] lastID = contract2.evaluateTransaction("getLastIDNumber");
            Integer lastIDInt = Integer.parseInt(new String(lastID))+1;
            String lastIDString = lastIDInt.toString();
            System.out.println(lastIDString);

            String path = "../Pictures/"+lastIDString+".png";

            /*System.out.println(id.getFullName());
            System.out.println(id.getAddress());
            System.out.println(id.getGender());
            System.out.println(id.getReligion());
            System.out.println(id.getJob());
            System.out.println(id.getMaritalStatus());
            System.out.println(id.getDateOfBirth());
            System.out.println(id.getPersonalPicture());*/
            contract.submitTransaction("issueID", id.getAddress(), id.getFullName(),
                    id.getGender(),id.getReligion(),id.getJob(), id.getMaritalStatus(),id.getDateOfBirth(), path);

            savePicture(id.getPersonalPicture(), lastIDString);

            return true;

        } catch (ContractException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean issueBirthCertificate(BirthCertificate birthCertificate) throws IOException {
        ConfigurationComponent configurationComponent = new ConfigurationComponent();
        Gateway gatewayConfig = configurationComponent.setupGatewayConfigurations();

        try (Gateway gateway = gatewayConfig) {

            Contract contract = configurationComponent.getContract(gateway, "BirthCertificateContract");

            contract.submitTransaction("issueBirthCertificate", birthCertificate.getFullName(), birthCertificate.getReligion(),
                    birthCertificate.getGender(),birthCertificate.getIdNumber(), birthCertificate.getDateOfBirth(),
                    birthCertificate.getBirthPlace(), birthCertificate.getNationality(), birthCertificate.getFullName(),
                    birthCertificate.getNationality(), birthCertificate.getReligion(), birthCertificate.getFullName(),
                    birthCertificate.getNationality(), birthCertificate.getReligion());
            return true;
        }
        catch (ContractException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean savePicture(String picture, String picName) throws IOException {
        picture = picture.replace("data:image/png;base64,","");
        //id.setPersonalPicture(x);

        byte[] decodedImg = Base64.getDecoder().decode(picture);
        //"/path/to/imageDir", "myImage.jpg"
        //"/home/arwa/go/fabric-samples/Passport_Verification_System/api"
        Path destinationFile = Paths.get("../Pictures",picName+".png");
        Files.write(destinationFile, decodedImg);

        return true;
    }
    private String getIdNumber() throws IOException {

        ConfigurationComponent configurationComponent = new ConfigurationComponent();
        Gateway gatewayConfig = configurationComponent.setupGatewayConfigurations();
        try (Gateway gateway = gatewayConfig) {

            Contract contract = configurationComponent.getContract(gateway,"IDContractFromHome");

            byte[] result;
            result = contract.evaluateTransaction("getLastIDNumber");
            return new String(result);

        } catch (ContractException e) {
            e.printStackTrace();
        }

        return "No ID Found";
    }
}
