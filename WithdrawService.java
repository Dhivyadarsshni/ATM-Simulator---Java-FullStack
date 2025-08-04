package com.ATMSimulator;

public class WithdrawService {
    public int balance(int deposit){
        int balance=0;
        if(deposit>0){
            balance+=deposit;
        }
        else {
              balance=0;
              System.out.println("Enter Valid amount");
        }
return balance;
    }
}
