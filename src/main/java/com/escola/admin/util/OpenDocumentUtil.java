package com.escola.admin.util;

import com.escola.admin.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;

public interface OpenDocumentUtil {
    static byte[] gerarPlanilha(String titulo, List<String> cabecalhos, List<String> colunas, List<JsonNode> dados) throws BaseException {
        try (OdfSpreadsheetDocument doc = OdfSpreadsheetDocument.newSpreadsheetDocument()) {

            OdfTable table = doc.getTableList().get(0);
            table.setTableName(titulo);
            table.removeRowsByIndex(0, table.getRowCount());
            OdfTableRow r = table.getRowByIndex(0);

            for (int numCol = 0; numCol < colunas.size(); ++numCol) {
                OdfTableCell c = r.getCellByIndex(numCol);
                c.setStringValue(cabecalhos.get(numCol) != null ? cabecalhos.get(numCol) : "");
            }

            Iterator<JsonNode> var12 = dados.iterator();

            while (var12.hasNext()) {
                JsonNode item = var12.next();
                r = table.appendRow();

                for (int numCol = 0; numCol < colunas.size(); ++numCol) {
                    String valor = item.get(colunas.get(numCol)) != null ? item.get(colunas.get(numCol)).asText() : "";
                    OdfTableCell c = r.getCellByIndex(numCol);
                    c.setStringValue(valor);
                }
            }

            ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
            doc.save(byteOS);
            byte[] bytes = byteOS.toByteArray();
            byteOS.close();
            return bytes;
        } catch (Exception ex) {
            throw new BaseException((ex.getMessage()));
        }
    }
}
