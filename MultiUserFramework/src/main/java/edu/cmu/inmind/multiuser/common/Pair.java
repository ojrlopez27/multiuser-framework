package edu.cmu.inmind.multiuser.common;

import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;

import java.util.Objects;

/**
 * Created by oscarr on 4/12/17.
 */
public class Pair<A, B> {
    public final A fst;
    public final B snd;

    public Pair(A var1, B var2) throws Throwable{
        this.fst = var1;
        this.snd = var2;
        if( var1 == null || var2 == null ){
            throw new MultiuserException( ErrorMessages.PAIR_VALUE_NULL, var1, var2);
        }
    }

    public String toString() {
        return "Pair[" + this.fst + "," + this.snd + "]";
    }

    public boolean equals(Object var1) {
        return var1 instanceof Pair && Objects.equals(this.fst, ((Pair)var1).fst)
                && Objects.equals(this.snd, ((Pair)var1).snd);
    }

    public int hashCode() {
        return this.fst == null?(this.snd == null?0:this.snd.hashCode() + 1)
                :(this.snd == null?this.fst.hashCode() + 2:this.fst.hashCode() * 17 + this.snd.hashCode());
    }

    public static <A, B> Pair<A, B> of(A var0, B var1) throws Throwable{
        return new Pair(var0, var1);
    }
}