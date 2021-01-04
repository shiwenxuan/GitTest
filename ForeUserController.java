package com.swx.mall.controller.force;

import com.alibaba.fastjson.JSONObject;
import com.swx.mall.controller.BaseController;
import com.swx.mall.entity.Address;
import com.swx.mall.entity.User;
import com.swx.mall.service.AddressService;
import com.swx.mall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class ForeUserController extends BaseController {
    @Resource
    private UserService userService;
    @Resource
    private AddressService addressService;
    //111

    //转到前台mall-用户详情页
    @RequestMapping(value = "userDetails",method = RequestMethod.GET)
    public String goToUserDetail(HttpSession session, Map<String,Object>map){
        logger.info("检查是否登录");
        Object userId=checkUser(session);
        if (userId!=null){
            logger.info("获取用户信息");
            User user=userService.get(Integer.parseInt(userId.toString()));
            map.put("user",user);
            logger.info("获取用户所在地区级地址");
            String districtAddressId=user.getUserAddress().getAddressAreaId();
            Address districtAddress=addressService.get(districtAddressId);
            logger.info("获取市集地址信息");
            Address cityAddress=addressService.get(districtAddress.getAddressRegionId().getAddressAreaId());
            logger.info("获取其他地址信息");
            List<Address> addressList=addressService.getRoot();
            List<Address> cityList=addressService.getList(null,
                    cityAddress.getAddressRegionId().getAddressAreaId());
            List<Address> districtList=addressService.getList(null,cityAddress.getAddressAreaId());
            map.put("addressList",addressList);
            map.put("cityList",cityList);
            map.put("districtList",districtList);
            map.put("addressId",cityAddress.getAddressRegionId().getAddressAreaId());
            map.put("cityAddressId",cityAddress.getAddressAreaId());
            map.put("districtAddressId",districtAddressId);
            return "fore/userDetails";
        }else {
            return "redirect:/login";
        }
    }
    //前台mall-用户更换头像
    @ResponseBody
    @RequestMapping(value = "user/uploadUserHeadImage",method = RequestMethod.POST,produces = "application/json;charset=utf-8")
    public String uploadUserHeadImage(@RequestParam MultipartFile file,HttpSession session){
        String originalFileName=file.getOriginalFilename();
        logger.info("获取图片原始文件名：{}",originalFileName);
        String extension=originalFileName.substring(originalFileName.lastIndexOf('.'));
        String fileName= UUID.randomUUID()+extension;
        String filePath=session.getServletContext().getRealPath("/")+
                "res/images/item/userProfilePicture/"+fileName;
        logger.info("文件上传路径：{}",filePath);
        JSONObject jsonObject=new JSONObject();
        try {
            logger.info("文件上传中");
            file.transferTo(new File(filePath));
            logger.info("文件上传成功");
            jsonObject.put("success",true);
            jsonObject.put("fileName",fileName);
        }catch (IOException e){
            logger.info("文件上传失败");
            e.printStackTrace();
            jsonObject.put("success",false);
        }
        return jsonObject.toJSONString();
    }
    //前台mall-用户详情更新
    @RequestMapping(value = "user/update",method = RequestMethod.POST,produces = "application/json;charset=utf-8")
    public String userUpdate(HttpSession session,Map<String,Object>map,
                             @RequestParam(value = "userNickName")String userNickName,
                             @RequestParam(value = "userRealName")String userRealName,
                             @RequestParam(value = "userGender")String userGender,
                             @RequestParam(value = "userBirthday")String userBirthday,
                             @RequestParam(value = "userAddress")String userAddress,
                             @RequestParam(value = "userProfilePictureSrc",required = false)String userProfilePictureSrc,
                             @RequestParam(value = "userPassword")String userPassword
                             )throws ParseException, UnsupportedEncodingException{
        logger.info("检查用户是否登录");
        Object userId=checkUser(session);
        if (userId!=null){
            logger.info("获取用户信息");
            User user=userService.get(Integer.parseInt(userId.toString()));
            map.put("user",user);
        }else {
            return "redirect:/login";
        }
        logger.info("创建用户对象");
        if (userProfilePictureSrc!=null&&userProfilePictureSrc.equals("")){
            userProfilePictureSrc=null;
        }
        User userUpdate=new User();
        userUpdate.setUserId(Integer.parseInt(userId.toString()));
        userUpdate.setUserNickName(new String(userNickName));
        userUpdate.setUserRealName(new String(userRealName));
        userUpdate.setUserGender(Byte.valueOf(userGender));
        userUpdate.setUserBirthday(new SimpleDateFormat("yyyy-MM-dd").parse(userBirthday));
        userUpdate.setUserAddress(new Address(userAddress));
        userUpdate.setUserProfilePictureSrc(userProfilePictureSrc);
        userUpdate.setUserPassword(userPassword);
        logger.info("执行修改");
        if (userService.update(userUpdate)){
            logger.info("修改成功");
            return "redirect:/userDetails";
        }
        throw  new RuntimeException();
    }
}
