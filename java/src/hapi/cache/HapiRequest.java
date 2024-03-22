/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hapi.cache;

import java.net.URL;

/**
 *
 * @author jbf
 */
public record HapiRequest ( 
        URL url, 
        String query,
        String dataset, 
        String start, 
        String stop, 
        String parameters, 
        String format ) {
    
}
