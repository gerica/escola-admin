package com.escola.admin.util;

import com.escola.admin.exception.BaseException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DataUtils {

    public static final String DD_MM_YYYY = "dd/MM/yyyy";

    private DataUtils() {
    }

    public static String getHoje() {
        return getHoje(DD_MM_YYYY);
    }

    public static String getHoje(String formater) {
        Instant now = Instant.now();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formater);
        return localDateTime.format(formatter);
    }

    public static LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static Date convertStringToDate(String dateInString) throws BaseException {
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm:ss a");
        try {
            return formatter.parse(dateInString);
        } catch (ParseException e) {
            throw new BaseException(e.getMessage());
        }
    }

    public static String formatar(Date date) {
        return formatar(date, DD_MM_YYYY);
    }

    public static String formatar(Date date, String format) {
        SimpleDateFormat dt1 = new SimpleDateFormat(format);
        return dt1.format(date);
    }

    public static Date setarHorarioLimiteInicial(Date data) throws BaseException {
        SimpleDateFormat formatoSemHorario = new SimpleDateFormat(DD_MM_YYYY);
        SimpleDateFormat formatoComHorario = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        if (data != null) {
            String dataSemHorario = formatoSemHorario.format(data);
            dataSemHorario += " 00:00:00";
            Date dataComHorario = null;
            try {
                dataComHorario = formatoComHorario.parse(dataSemHorario);
            } catch (ParseException e) {
                throw new BaseException(e.getMessage());
            }
            return dataComHorario;
        }
        return null;
    }

    public static Date setarHorarioLimiteFinal(Date data) throws BaseException {
        SimpleDateFormat formatoSemHorario = new SimpleDateFormat(DD_MM_YYYY);
        SimpleDateFormat formatoComHorario = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        if (data != null) {
            String dataSemHorario = formatoSemHorario.format(data);
            dataSemHorario += " 23:59:59";
            Date dataComHorario = null;

            try {
                dataComHorario = formatoComHorario.parse(dataSemHorario);
            } catch (ParseException e) {
                throw new BaseException(e.getMessage());
            }

            return dataComHorario;
        }
        return null;
    }

    public static boolean isDataAMaiorQueDataB(Date dataA, Date dataB) {
        if (dataB == null || dataA == null) {
            return false; // Handle null case safely
        }
        return dataB.after(dataA);
    }

}
