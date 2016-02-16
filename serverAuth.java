import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;

/***************************************************************************************************
 * This Class is responsible for Authentication, verification and creation of users' credentials.  *
 * Verified usernames and passwords are stored in a format of "username:password" in a single File *
 * which has been created during class instantiation.                                              *
 *                                                                                                 *
 ***************************************************************************************************
 *                                      AUTHENTICATION CLASS                                       *
 *                                                                                                 *
 ***************************************************************************************************
 */
public class serverAuth {
    private File userAndPass = null;
    private String username = null;
    private String password = null;
    private FileWriter fileWriter = null;
    //==================================  Constructor  ===================================
    public serverAuth (String filename) {
        this.userAndPass = new File(filename);

    }
    //=================================== Creates users ====================================
    public synchronized boolean createUser (String username,String password) throws NoSuchAlgorithmException {
        boolean flagNotExist = false;
        String[] userPassCombination = null;
        this.username = username.toLowerCase();
        this.password = password;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.userAndPass)));
            fileWriter = new FileWriter(userAndPass.getAbsoluteFile(),true);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                userPassCombination = line.split(":");
                if (userPassCombination[0].equalsIgnoreCase(this.username)) {
                    flagNotExist = false;
                    break;
                } else {
                    flagNotExist = true;
                    continue;
                }
            }
            if (flagNotExist == true || userAndPass.length() == 0) { //if username does not already exist or file is empty

                //**********This part is taken from www.OWASP.org**************
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                //64-bit salt
                byte[] bSalt = new byte[8];
                random.nextBytes(bSalt);
                //Create a hash from salted password string
                byte[] bDigest = getHash(100,this.password,bSalt);
                String sDigest = byteToBase64(bDigest);
                String sSalt = byteToBase64(bSalt);
                //*************************************************************

                String temp = this.username + ":" + sDigest + ":" + sSalt;
                fileWriter.write("\r\n");
                fileWriter.write(temp);
                flagNotExist = true; //when file is empty
            }
            bufferedReader.close();
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return flagNotExist;

    }

    //================================== Authentication ==================================
    public synchronized boolean isAuthenticated (String username,String password) throws NoSuchAlgorithmException {
        boolean authFlag = false;
        String[] userPassCombination = null;
        this.username = username.toLowerCase();
        this.password = password;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.userAndPass)));
            String line;
            while ((line = bufferedReader.readLine()) != null){
                userPassCombination = line.split(":");
                //taken from www.OWASP.org
                if (userPassCombination[0].equalsIgnoreCase(this.username)){
                    byte[] bDigest = base64ToByte(userPassCombination[1]);
                    byte[] bSalt = base64ToByte(userPassCombination[2].replace("\r\n",""));
                    byte[] proposedDigest = getHash(100, password, bSalt);
                    if (Arrays.equals(bDigest,proposedDigest)) {
                        authFlag = true;
                        break;
                    } else
                        authFlag = false;
                }
                else
                    authFlag = false;
            }
            bufferedReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return authFlag;
    }

    //=================== Username contains characters or digits only ====================
    public synchronized boolean containsNumCharOnly (String username) {
        boolean flag = true;
        if (username.isEmpty()) {
            flag = false;
        }
        else {
            for (char c:username.toCharArray()){
                if (Character.isLetterOrDigit(c)){
                    flag = true;
                    continue;
                }
                else{
                    flag = false;
                    break;
                }
            }
        }
        return flag;
    }
    //============================== Check password condition ==============================
    public synchronized boolean meetPassCondition (String password) {
        boolean flag = false;
        Pattern lowerCase = Pattern.compile("[a-z]");
        Pattern upperCase = Pattern.compile("[A-Z]");
        Pattern containsDigit = Pattern.compile("[\\d]");
        Pattern containsSpecial = Pattern.compile("[-+_!@#$%^&*.,?]");

        if (!password.isEmpty() && password.length() >= 8) { //not empty and has more than 8 characters
            if (lowerCase.matcher(password).find() && upperCase.matcher(password).find() && containsDigit.matcher(password).find() && containsSpecial.matcher(password).find())
                flag = true;
        } else
            flag = false;

        return flag;

    }

    /***************************************************************************************************
     *                                                                                                 *
     * This part is taken from www.OWASP.org and it implements hashing with salts for password hashes. *
     *                                                                                                 *
     ***************************************************************************************************
     */
    public synchronized byte[] getHash(int iterationNb, String password, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(salt);
        byte[] input = digest.digest(password.getBytes("UTF-8"));
        for (int i = 0; i < iterationNb; i++) {
            digest.reset();
            input = digest.digest(input);
        }
        return input;
    }
    public synchronized static String byteToBase64(byte[] data){
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }
    public synchronized static byte[] base64ToByte(String data) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(data);
    }

}
