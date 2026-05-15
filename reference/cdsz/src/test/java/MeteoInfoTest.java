//import org.meteoinfo.common.Extent;
//import org.meteoinfo.data.GridData;
//import org.meteoinfo.data.meteodata.MeteoDataInfo;
//import org.meteoinfo.geo.layer.RasterLayer;
//import org.meteoinfo.geo.layout.MapLayout;
//import org.meteoinfo.geo.mapview.MapView;
//import org.meteoinfo.geo.meteodata.DrawMeteoData;
//
//import java.awt.*;
//
//public class MeteoInfoTest {
//    public static void main(String[] args) throws Exception {
//        MeteoDataInfo meteo = new MeteoDataInfo();
////        meteo.openAWXData("D:\\Download\\ANI_VIS_R04_20210812_0800_FY2G.AWX");
//        meteo.openGRIBData("E:/fl/datas/scmoc/Z_NWGD_C_BABJ_20240922040736_P_RFFC_SCMOC-PPH-1H_202409220800_24001.GRB2", 2);
//        GridData grid = meteo.getGridData();
//        //色阶文件
//        String colorPath = "D:\\apache-tomcat-8.0.50\\alt色阶\\AWX.pal";
////        DrawMeteoData.createRasterLayer(grid, "");
//        //绘制图层
//        RasterLayer layer = DrawMeteoData.createRasterLayer(grid, "");
//        //创建视图
//        MapView view = new MapView();
//        //叠加图层
//        view.addLayer(layer);
//        MapLayout layout  = new MapLayout();
//        //去除图形边框
//        layout.getActiveMapFrame().setDrawNeatLine(false);
//        //区域边界
//        Extent extent = view.getExtent();
//        //设置矩形的宽和高
//        Rectangle bounds = new Rectangle(800, (int) (800 * 1D / extent.getWidth() * extent.getHeight()));
//        //设置地图边框
//        layout.setPageBounds(new Rectangle(0, 0, bounds.width, bounds.height));
//        //设置页面边框
//        layout.getActiveMapFrame().setLayoutBounds(new Rectangle(0, 0, bounds.width, bounds.height));
//        layout.getActiveMapFrame().setMapView(view);
//        layout.exportToPicture("2.png");
//    }
//}
