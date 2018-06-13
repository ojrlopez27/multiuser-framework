package edu.cmu.inmind.multiuser.controller.composer.simulation;

/**
 * Created by oscarr on 5/25/18.
 */
public class SimuConstants {

    public final static int S1_BOB_STARTS = 1;
    public final static int S2_ALICE_LOCATION = 2;
    public final static int S3_BOB_LOCATION = 3;
    public final static int S4_ALICE_FIND_GROCERY = 4;
    public final static int S5_ALICE_DIST_GROCERY = 5;
    public final static int S6_BOB_FIND_GROCERY = 6;
    public final static int S7_BOB_DIST_GROCERY = 7;
    public final static int S8_ALICE_SHOP_LIST = 8;
    public final static int S9_CLOSER_TO_GROCERY = 9;
    public final static int S10_BOB_DO_GROCERY = 10;
    public final static int S11_ALICE_ADD_PREF = 11;
    public final static int S12_ALICE_DO_GROCERY = 12;
    public final static int S13_BOB_FIND_BEER = 13;
    public final static int S14_BOB_GO_BEER_SHOP = 14;
    public final static int S15_BOB_FIND_HOME_DECO = 15;
    public final static int S16_BOB_GO_HOME_DECO = 16;
    public final static int S17_ALICE_HEADACHE = 17;
    public final static int S18_BOB_COUPONS = 18;
    public final static int S19_BOB_GO_HOME_DECO = 19;
    public final static int S20_ALICE_GO_HOME_DECO = 20;


    /** Simulation steps. */
    public enum SimSteps {
        S0_BOB_STARTS,

        S1_ALICE_LOCATION,

        S2_BOB_LOCATION,

        S3_ALICE_FIND_GROCERY,

        S4_ALICE_DIST_GROCERY,

        S5_BOB_FIND_GROCERY,

        S6_BOB_DIST_GROCERY,

        S7_CLOSER_TO_GROCERY,

        S8_ALICE_SHARE_SHOP_LIST,

        S9_BOB_DO_GROCERY,

        S9_1_BOB_MOVE_TO_GROCERY,

        S10_ALICE_ADD_PREF,

        S11_ALICE_DO_GROCERY,

        S11_1_ALICE_MOVE_TO_GROCERY,

        S12_BOB_FIND_BEER,

        S13_BOB_GO_BEER_SHOP,

        S13_1_BOB_MOVE_BEER_SHOP,

        S13_2_ALICE_AT_SUPERMARKET,

        S14_BOB_FIND_HOME_DECO,

        S15_BOB_GO_HOME_DECO,

        S15_1_BOB_MOVE_HOME_DECO,

        S16_ALICE_HEADACHE,

        S17_BOB_COUPONS,

        S18_BOB_GO_HOME_DECO,

        S19_ALICE_GO_HOME_DECO,

        S20_GO_HOME,

        S21_AT_HOME
    }

}
