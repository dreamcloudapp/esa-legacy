package com.dreamcloud.esa.annoatation;

/**
 * Takes a stripped dump file and disambiguation information
 * and adds the following information:
 * <docs>
 *     <doc>
 *         <title>Cat</title>
 *         <text>Cats are small, furry, and cute mammals.</text>
 *         <incomingLinks>24</incomingLinks>
 *         <outgoingLinks>24</incomingLinks>
 *         <terms>1492</terms>
 *     </doc>
 * </docs>
 *
 * This will be saved as an XML,
 * with the option to exclude things that don't meed the minimum criteria.
 * This results in a smaller file size,
 * but makes the dump less versatile.
 */
public class WikiAnnotator {
}
