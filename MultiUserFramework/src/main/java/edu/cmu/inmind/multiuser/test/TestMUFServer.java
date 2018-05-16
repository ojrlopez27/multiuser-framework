package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.muf.MUFLifetimeManager;
import edu.cmu.inmind.multiuser.controller.muf.MultiuserController;

import java.net.InetAddress;
import java.util.Scanner;

public class TestMUFServer {
    public static void main(String args[]) throws Exception {
        MultiuserController muf;
        try {
             muf = MUFLifetimeManager.startFramework(
                    TestUtils.getModules(TestOrchestrator.class ),
                    TestUtils.createConfig( "tcp://"+
                                    InetAddress.getLocalHost().getHostAddress(),
                            5555 ).setTCPon( true ) );
            String command ="";
            try (Scanner scanner = new Scanner(System.in)) {
                while (!command.equals("shutdown")) {
                    if (scanner.hasNextLine()) {
                        command = scanner.nextLine();
                        if (command.equals("shutdown")) {
                            MUFLifetimeManager.stopFramework(muf);
                        }
                        System.err.println("Type shutdown to stop:");
                    } else {
                        Utils.sleep(300);
                    }
                }
            }
        }catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
    }

}
