package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.client.DummyClient;
import edu.cmu.inmind.multiuser.controller.common.Utils;

import java.util.Scanner;

/**
 * Created by oscarr on 5/16/18.
 */
public class DummyTest {

    public static void main(String args[]){
        DummyClient dummy = new DummyClient();
        dummy.test();
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("Enter something: ");
            String command = scanner.nextLine();
            if(command.equals("stop"))
                dummy.disconnect();
            Utils.sleep(10);
        }
    }
}
