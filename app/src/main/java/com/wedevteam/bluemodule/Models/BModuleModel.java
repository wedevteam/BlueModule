package com.wedevteam.bluemodule.Models;

import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

public class BModuleModel {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String Nome;
    public String MACAddress;
    public String Alias;
    public String Stato;
    public String Tipo;
    public String UUID;
    public String PW1;
    public String PW2;
    public String NuovoNome;
    public String DataAttivazione;
    public String MSG;

    public BModuleModel() {

    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getNome() {
        return Nome;
    }
    public void setNome(String nome) {
        Nome = nome;
    }
    public String getMACAddress() {
        return MACAddress;
    }
    public void setMACAddress(String MACAddress) {
        this.MACAddress = MACAddress;
    }
    public String getAlias() {
        return Alias;
    }
    public void setAlias(String alias) {
        Alias = alias;
    }
    public String getStato() {
        return Stato;
    }
    public void setStato(String stato) {
        Stato = stato;
    }
    public String getTipo() {
        return Tipo;
    }
    public void setTipo(String tipo) {
        Tipo = tipo;
    }
    public String getUUID() {
        return UUID;
    }
    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
    public String getPW1() {
        return PW1;
    }
    public void setPW1(String PW1) {
        this.PW1 = PW1;
    }
    public String getPW2() {
        return PW2;
    }
    public void setPW2(String PW2) {
        this.PW2 = PW2;
    }
    public String getNuovoNome() {
        return NuovoNome;
    }
    public void setNuovoNome(String nuovoNome) {
        NuovoNome = nuovoNome;
    }
    public String getDataAttivazione() {
        return DataAttivazione;
    }
    public void setDataAttivazione(String dataAttivazione) {
        DataAttivazione = dataAttivazione;
    }
    public String getMSG() {
        return MSG;
    }
    public void setMSG(String MSG) {
        this.MSG = MSG;
    }
}
