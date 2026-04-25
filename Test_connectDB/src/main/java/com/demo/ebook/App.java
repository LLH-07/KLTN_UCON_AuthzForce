/*
    File này chạy demo
*/

package com.demo.ebook;

public class App {
    public static void main(String[] args) throws Exception {
        AuthzforceEvaluator evaluator = new AuthzforceEvaluator();

        System.out.println("CASE 1 => " + evaluator.evaluate(1, 3)); // expect Permit
        System.out.println("CASE 2 => " + evaluator.evaluate(1, 8)); // expect Deny
        System.out.println("CASE 3 => " + evaluator.evaluate(2, 2)); // expect Deny
        System.out.println("CASE 4 => " + evaluator.evaluate(3, 4)); // expect Permit
        System.out.println("CASE 5 => " + evaluator.evaluate(4, 1)); // expect Deny
    }
}